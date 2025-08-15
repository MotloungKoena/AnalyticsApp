/*package motloung.koena.analyticsapp.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
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
import motloung.koena.analyticsapp.data.Event
import motloung.koena.analyticsapp.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ensureNotificationPermission();
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.rv.layoutManager = LinearLayoutManager(this)
        binding.rv.adapter = adapter

        lifecycleScope.launchWhenStarted {
            vm.events.collect { adapter.submitList(it) }
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


        /*binding.btnShare.setOnClickListener {
            val csv = toCsv(adapter.currentList)
            val i = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "Analytics Export")
                putExtra(Intent.EXTRA_TEXT, csv) // share content directly
            }
            startActivity(Intent.createChooser(i, "Share CSV"))
        }*/

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
                        0 -> { // CSV
                            val i = Intent(Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(Intent.EXTRA_SUBJECT, "Analytics Export")
                                putExtra(Intent.EXTRA_TEXT, csv)
                            }
                            startActivity(Intent.createChooser(i, "Share CSV"))
                        }
                        1 -> { // PDF
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

    fun generatePdf(context: Context, csvText: String): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = pdfDocument.startPage(pageInfo)

        val paint = Paint()
        val textArray = csvText.split("\n")
        var yPos = 30f

        for (line in textArray) {
            page.canvas.drawText(line, 10f, yPos, paint)
            yPos += paint.descent() - paint.ascent()
        }

        pdfDocument.finishPage(page)

        val file = File(context.getExternalFilesDir(null), "analytics_events.pdf")
        FileOutputStream(file).use { pdfDocument.writeTo(it) }

        pdfDocument.close()
        return file
    }

    fun sharePdf(context: Context, file: File) {
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


}*/

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

    // ==== Service IPC (Messenger) ====
    private var serviceMessenger: Messenger? = null
    private var isBound = false

    private val incomingHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            if (msg.what == 1) { // MSG_GET_SUMMARY
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

    /*private val conn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            serviceMessenger = Messenger(binder)
            isBound = true
            requestFinanceSummary()
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            serviceMessenger = null
        }
    }*/
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

    /*private fun bindFinanceService() {
        val i = Intent("motloung.koena.financeapp.ACTION_SUMMARY_SERVICE").apply {
            setPackage("motloung.koena.financeapp") // explicit target
        }
        try {
            bindService(i, conn, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            Toast.makeText(this, "Bind failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }*/
    /*private fun bindFinanceService() {
        val i = Intent("motloung.koena.financeapp.ACTION_SUMMARY_SERVICE")
            .setPackage("motloung.koena.financeapp") // explicit & secure
        try {
            val ok = bindService(i, conn, Context.BIND_AUTO_CREATE)
            if (!ok) {
                Toast.makeText(this, "bindService() returned false", Toast.LENGTH_LONG).show()
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, "SecurityException: ${e.message}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Bind failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }*/
    private val ACTION_SUMMARY = "motloung.koena.financeapp.ACTION_SUMMARY_SERVICE"

    private fun findFinanceService(): ComponentName? {
        val intent = Intent(ACTION_SUMMARY)

        val results = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // API 33+
            packageManager.queryIntentServices(
                intent,
                PackageManager.ResolveInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentServices(intent, 0)  // API 32 and lower
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

    /*private fun bindFinanceService() {
        // Strongly explicit binding
        val comp = ComponentName(
            "motloung.koena.financeapp",
            "motloung.koena.financeapp.service.FinanceSummaryService"
        )

        val i = Intent("motloung.koena.financeapp.ACTION_SUMMARY_SERVICE").apply {
            component = comp
            `package` = "motloung.koena.financeapp"
        }

        try {
            val ok = bindService(i, conn, Context.BIND_AUTO_CREATE)
            if (!ok) {
                Toast.makeText(this, "bindService() returned false", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Binding Finance service…", Toast.LENGTH_SHORT).show()
            }
        } catch (se: SecurityException) {
            Toast.makeText(this, "SecurityException: ${se.message}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Bind failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }*/
    /*private fun bindFinanceService() {
        val pkg = "motloung.koena.financeapp"
        val cls = "motloung.koena.financeapp.service.FinanceSummaryService"

        // Strict explicit component
        val i = Intent().apply {
            setClassName(pkg, cls)     // <- this alone is enough
            // Optionally also include the action your service checks:
            action = "motloung.koena.financeapp.ACTION_SUMMARY_SERVICE"
        }

        try {
            val ok = bindService(i, conn, Context.BIND_AUTO_CREATE)
            if (!ok) {
                Toast.makeText(this, "bindService() returned false", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Binding Finance service…", Toast.LENGTH_SHORT).show()
            }
        } catch (se: SecurityException) {
            Toast.makeText(this, "SecurityException: ${se.message}", Toast.LENGTH_LONG).show()
        }
    }*/
    /*private fun bindFinanceService() {
        val comp = findFinanceService()
        if (comp == null) {
            // Helpful hint: likely FinanceApp not installed or manifest/action mismatch
            android.widget.Toast.makeText(
                this,
                "Finance service not found. Is FinanceApp installed & exported?",
                android.widget.Toast.LENGTH_LONG
            ).show()
            return
        }

        // Build an explicit intent using the discovered component + required action
        val i = Intent().apply {
            component = comp
            action = ACTION_SUMMARY
        }

        try {
            val ok = bindService(i, conn, BIND_AUTO_CREATE)
            if (!ok) {
                android.widget.Toast.makeText(
                    this,
                    "bindService() returned false (component=${comp.packageName}/${comp.className})",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            } else {
                android.widget.Toast.makeText(
                    this,
                    "Binding ${comp.packageName}/${comp.className}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        } catch (se: SecurityException) {
            android.widget.Toast.makeText(this, "SecurityException: ${se.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }*/


    /*  private fun requestFinanceSummary() {
        val msg = Message.obtain(null, 1) // MSG_GET_SUMMARY
        msg.replyTo = clientMessenger
        try {
            serviceMessenger?.send(msg)
        } catch (e: Exception) {
            Toast.makeText(this, "Request failed: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            // We can unbind immediately after the request if you prefer short-lived connections:
            unbindIfBound()
        }
    }*/
  private fun requestFinanceSummary() {
      val msg = Message.obtain(null, 1) // MSG_GET_SUMMARY
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
    private fun debugInspectFinanceProvider() {
        val auth = "motloung.koena.financeapp.provider"
        val pi = if (Build.VERSION.SDK_INT >= 33) {
            packageManager.resolveContentProvider(auth, PackageManager.ComponentInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.resolveContentProvider(auth, 0)
        }

        if (pi == null) {
            Toast.makeText(this, "Provider NOT found: $auth", Toast.LENGTH_LONG).show()
            return
        }

        val msg = """
        Provider: ${pi.packageName}/${pi.name}
        exported=${pi.exported}
        readPermission=${pi.readPermission}
        writePermission=${pi.writePermission}
        grantUriPermissions=${pi.grantUriPermissions}
    """.trimIndent()

        android.util.Log.d("DBG", msg)
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }


    private fun queryFinanceProvider() {
        val uri = Uri.parse("content://motloung.koena.financeapp.provider/events")
        try {
            contentResolver.query(uri, null, null, null, "receivedAt DESC")?.use { c ->
                if (c.count == 0) {
                    Toast.makeText(this, "No events", Toast.LENGTH_SHORT).show()
                    return
                }
                val idIdx = c.getColumnIndex("id")
                val typeIdx = c.getColumnIndex("type")
                val payloadIdx = c.getColumnIndex("payload")
                val tsIdx = c.getColumnIndex("receivedAt")

                val sb = StringBuilder()
                while (c.moveToNext()) {
                    val id = if (idIdx >= 0) c.getLong(idIdx) else -1
                    val type = if (typeIdx >= 0) c.getString(typeIdx) else ""
                    val payload = if (payloadIdx >= 0) c.getString(payloadIdx) else ""
                    val ts = if (tsIdx >= 0) c.getLong(tsIdx) else 0L
                    sb.append("#$id [$type] $payload @ $ts\n")
                }
                showSummaryDialog("Finance provider results", sb.toString())
            }
        } catch (se: SecurityException) {
            Toast.makeText(this, "Denied: ${se.message ?: "Permission denial"}", Toast.LENGTH_LONG).show()
        } catch (t: Throwable) {
            Toast.makeText(this, "Error: ${t.message}", Toast.LENGTH_LONG).show()
        }
    }
    private fun showSummaryDialog(title: String, body: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(if (body.isBlank()) "(no data)" else body)
            .setPositiveButton("OK", null)
            .show()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ensureNotificationPermission()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false) // No back arrow

        binding.btnFetchFinanceSummary.setOnClickListener {
            //val diag = debugResolveFinanceService()
            //Toast.makeText(this, diag, Toast.LENGTH_LONG).show()

            bindFinanceService()
        }

        binding.btnOpenFinance.setOnClickListener {
            // Explicit Activity intent to FinanceApp
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
            debugCheckFinanceReadPerm() //shoould remove this line, i used it to debug, check for errors
            //debugInspectFinanceProvider()//also remove this, for debugging
            //val uri = android.net.Uri.parse("content://motloung.koena.financeapp.provider/events")
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

        // Live updates + empty state check
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

        // Clear history confirmation
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

        // Share as CSV or PDF
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
                        0 -> { // CSV
                            val i = Intent(Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(Intent.EXTRA_SUBJECT, "Analytics Export")
                                putExtra(Intent.EXTRA_TEXT, csv)
                            }
                            startActivity(Intent.createChooser(i, "Share CSV"))
                        }
                        1 -> { // PDF
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


    /*private fun generatePdf(context: Context, csvText: String): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        val page = pdfDocument.startPage(pageInfo)

        val paint = Paint()
        val textArray = csvText.split("\n")
        var yPos = 30f

        for (line in textArray) {
            page.canvas.drawText(line, 10f, yPos, paint)
            yPos += paint.descent() - paint.ascent()
        }

        pdfDocument.finishPage(page)

        val file = File(context.getExternalFilesDir(null), "analytics_events.pdf")
        FileOutputStream(file).use { pdfDocument.writeTo(it) }
        pdfDocument.close()

        return file
    }*/

    private fun generatePdf(context: Context, csvText: String): File {
        // --- Page setup (A4 size at ~72 dpi) ---
        val pageWidth = 595
        val pageHeight = 842
        val margin = 40f
        val headerGap = 12f
        val bodyTop = 120f
        val footerBottom = pageHeight - 24f

        // --- Paint styles ---
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

        // --- Metadata ---
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

        // Header on first page
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

