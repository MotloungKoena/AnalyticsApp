package motloung.koena.analyticsapp.ipc

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import motloung.koena.analyticsapp.data.AnalyticsDb
import motloung.koena.analyticsapp.data.Event

class BudgetReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.example.ACTION_BUDGET_REMINDER") return
        val msg = intent.getStringExtra("message") ?: return

        // 1) Persist in Room (IO thread)
        CoroutineScope(Dispatchers.IO).launch {
            AnalyticsDb.get(context).eventDao().insert(
                Event(type = "BUDGET_REMINDER", payload = msg)
            )
        }

        // 2) Notify user (guarded for Android 13+ permission)
        val canNotify = if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        if (canNotify) {
            val channelId = "analytics_events"
            val nm = NotificationManagerCompat.from(context)
            nm.createNotificationChannel(
                NotificationChannelCompat.Builder(
                    channelId,
                    NotificationManagerCompat.IMPORTANCE_DEFAULT
                ).setName("Analytics Events").build()
            )

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Budget Reminder")
                .setContentText(msg)
                .setAutoCancel(true)
                .build()

            nm.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
        }
    }
}
