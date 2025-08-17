package motloung.koena.analyticsapp.ui

 import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
 import android.net.Uri
 import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import motloung.koena.analyticsapp.data.Event
import motloung.koena.analyticsapp.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val vm: MainViewModel by viewModels()
    private val adapter = EventAdapter()

    private val requestPostNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) requestPostNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private var serviceMessenger: Messenger? = null
    private var isBound = false

    private val incomingHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            if (msg.what == 1) {
                val summary = msg.data?.getString("summary") ?: "No summary"
                Toast.makeText(this@MainActivity, "Service replied", Toast.LENGTH_SHORT).show()
                androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                    .setTitle("Finance Summary")
                    .setMessage(summary)
                    .setPositiveButton("OK", null)
                    .show()
            } else {
                super.handleMessage(msg)
            }
        }
    }
    private val clientMessenger = Messenger(incomingHandler)

    private val conn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            isBound = true
            serviceMessenger = Messenger(binder)
            Toast.makeText(this@MainActivity, "Service connected", Toast.LENGTH_SHORT).show()
            requestFinanceSummary()
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            serviceMessenger = null
            Toast.makeText(this@MainActivity, "Service disconnected", Toast.LENGTH_SHORT).show()
        }
    }

    private val ACTION_SUMMARY = "motloung.koena.financeapp.ACTION_SUMMARY_SERVICE"

    private fun findFinanceService(): ComponentName? {
        val intent = Intent(ACTION_SUMMARY)

        val results = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentServices(
                intent,
                PackageManager.ResolveInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentServices(intent, 0)
        }

        if (results.isNullOrEmpty()) return null
        val si = results.first().serviceInfo
        return ComponentName(si.packageName, si.name)
    }

    private fun bindFinanceService() {
        val comp = findFinanceService()
        if (comp == null) {
            Toast.makeText(
                this,
                "Finance service not found. Is FinanceApp installed & exported?",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val i = Intent().apply {
            component = comp
            action = ACTION_SUMMARY
        }

        try {
            val ok = bindService(i, conn, BIND_AUTO_CREATE)
            if (!ok) {
                Toast.makeText(
                    this,
                    "bindService() returned false (${comp.packageName}/${comp.className})",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    this,
                    "Binding ${comp.packageName}/${comp.className}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (se: SecurityException) {
            Toast.makeText(this, "SecurityException: ${se.message}", Toast.LENGTH_LONG).show()
        }
    }

  private fun requestFinanceSummary() {
      val msg = Message.obtain(null, 1)
      msg.replyTo = clientMessenger
      try {
          serviceMessenger?.send(msg)
      } catch (e: Exception) {
          Toast.makeText(this, "Request failed: ${e.message}", Toast.LENGTH_LONG).show()
      }
  }

    private fun unbindIfBound() {
        if (isBound) {
            try { unbindService(conn) } catch (_: Exception) {}
            isBound = false
            serviceMessenger = null
        }
    }
    private fun debugCheckFinanceReadPerm() {
        val perm = "com.example.permission.READ_FINANCE_DATA"
        val pmResult = packageManager.checkPermission(perm, packageName)
        val has = (pmResult == PackageManager.PERMISSION_GRANTED)
        Toast.makeText(this, "Has READ_FINANCE_DATA = $has", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ensureNotificationPermission()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false) // No back arrow

        binding.btnFetchFinanceSummary.setOnClickListener {
            bindFinanceService()
        }

        binding.btnOpenFinance.setOnClickListener {
            val i = Intent().apply {
                setClassName(
                    "motloung.koena.financeapp",
                    "motloung.koena.financeapp.ui.MainActivity"
                )
                action = "motloung.koena.financeapp.VIEW_FINANCE"
                setPackage("motloung.koena.financeapp")
            }
            try {
                startActivity(i)
            } catch (_: Exception) {
                Toast.makeText(this, "FinanceApp not installed", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnQueryFinanceProvider.setOnClickListener {
            debugCheckFinanceReadPerm()
            val uri = Uri.parse("content://motloung.koena.financeapp.financeprovider/events")

            val cols = arrayOf("id", "type", "payload", "receivedAt")

            try {
                contentResolver.query(uri, cols, null, null, "receivedAt DESC")?.use { c ->
                    val count = c.count
                    var firstLine = "(none)"
                    if (c.moveToFirst()) {
                        val id = c.getLong(0)
                        val type = c.getString(1)
                        val payload = c.getString(2)
                        val ts = c.getLong(3)
                        firstLine = "id=$id, type=$type, payload=$payload, receivedAt=$ts"
                    }
                    androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Finance Provider Result")
                        .setMessage("Rows: $count\nFirst: $firstLine")
                        .setPositiveButton("OK", null)
                        .show()
                } ?: run {
                    Toast.makeText(this, "Query failed (permission/validation?)", Toast.LENGTH_LONG).show()
                }
            } catch (e: SecurityException) {
                Toast.makeText(this, "Denied: ${e.message}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }


        binding.rv.layoutManager = LinearLayoutManager(this)
        binding.rv.adapter = adapter

        lifecycleScope.launchWhenStarted {
            vm.events.collect { events ->
                adapter.submitList(events)

                if (events.isEmpty()) {
                    binding.txtEmpty.visibility = View.VISIBLE
                    binding.rv.visibility = View.GONE
                } else {
                    binding.txtEmpty.visibility = View.GONE
                    binding.rv.visibility = View.VISIBLE
                }
            }
        }

        binding.btnClear.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Clear History")
                .setMessage("Are you sure you want to delete all analytics events? This cannot be undone.")
                .setPositiveButton("Yes") { _, _ ->
                    lifecycleScope.launch { vm.clear() }
                    Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.btnShare.setOnClickListener {
            val items = adapter.currentList
            if (items.isEmpty()) {
                Toast.makeText(this, "No data to export", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val csv = toCsv(items)

            MaterialAlertDialogBuilder(this)
                .setTitle("Export as")
                .setItems(arrayOf("CSV", "PDF")) { _, which ->
                    when (which) {
                        0 -> {
                            val i = Intent(Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(Intent.EXTRA_SUBJECT, "Analytics Export")
                                putExtra(Intent.EXTRA_TEXT, csv)
                            }
                            startActivity(Intent.createChooser(i, "Share CSV"))
                        }
                        1 -> {
                            lifecycleScope.launch(Dispatchers.IO) {
                                val file = generatePdf(this@MainActivity, csv)
                                withContext(Dispatchers.Main) {
                                    sharePdf(this@MainActivity, file)
                                }
                            }
                        }
                    }
                }
                .show()
        }
    }

    private fun toCsv(items: List<Event>): String {
        val sb = StringBuilder("id,type,payload,receivedAt\n")
        items.forEach { e ->
            val payloadEsc = e.payload.replace("\"", "\"\"")
            sb.append("${e.id},${e.type},\"$payloadEsc\",${e.receivedAt}\n")
        }
        return sb.toString()
    }

    override fun onDestroy() {
        unbindIfBound()
        super.onDestroy()
    }

    private fun generatePdf(context: Context, csvText: String): File {
        val pageWidth = 595
        val pageHeight = 842
        val margin = 40f
        val headerGap = 12f
        val bodyTop = 120f
        val footerBottom = pageHeight - 24f

        val titlePaint = Paint().apply {
            isAntiAlias = true
            textSize = 18f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT_BOLD, android.graphics.Typeface.BOLD)
        }
        val metaPaint = Paint().apply {
            isAntiAlias = true
            textSize = 12f
        }
        val linePaint = Paint().apply { strokeWidth = 1f }
        val bodyPaint = Paint().apply {
            isAntiAlias = true
            textSize = 10f
            typeface = android.graphics.Typeface.MONOSPACE
        }
        val footerPaint = Paint().apply {
            isAntiAlias = true
            textSize = 10f
        }

        val lines = csvText.split("\n")
        val totalRows = (lines.size - 1).coerceAtLeast(0)
        val now = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date())

        val pdf = PdfDocument()

        var pageNum = 1
        var y = bodyTop
        var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
        var canvas = page.canvas

        fun drawHeader(c: android.graphics.Canvas) {
            c.drawText("Analytics Events Export", margin, 50f, titlePaint)
            c.drawText("Generated: $now", margin, 50f + headerGap + 12f, metaPaint)
            c.drawText("Total rows: $totalRows", margin, 50f + 2 * (headerGap + 12f), metaPaint)
            c.drawLine(margin, bodyTop - 10f, pageWidth - margin, bodyTop - 10f, linePaint)
        }

        fun drawFooter(c: android.graphics.Canvas) {
            val text = "Page $pageNum"
            val textWidth = footerPaint.measureText(text)
            c.drawText(text, pageWidth - margin - textWidth, footerBottom, footerPaint)
        }

        drawHeader(canvas)

        val lineHeight = bodyPaint.descent() - bodyPaint.ascent()

        lines.forEach { raw ->
            val line = raw.trimEnd()
            if (line.isEmpty()) return@forEach

            if (y + lineHeight > footerBottom - 10f) {
                drawFooter(canvas)
                pdf.finishPage(page)
                pageNum++
                y = bodyTop
                page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
                canvas = page.canvas
                drawHeader(canvas)
            }

            canvas.drawText(line, margin, y, bodyPaint)
            y += lineHeight
        }

        drawFooter(canvas)
        pdf.finishPage(page)

        val file = File(context.getExternalFilesDir(null), "analytics_events_${now.replace(" ", "_").replace(":", "")}.pdf")
        FileOutputStream(file).use { pdf.writeTo(it) }
        pdf.close()

        return file
    }


    private fun sharePdf(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share PDF"))
    }
}

