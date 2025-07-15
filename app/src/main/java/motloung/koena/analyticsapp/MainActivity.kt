package motloung.koena.analyticsapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var txtBroadcastMessage: TextView

    private val budgetReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val message = intent?.getStringExtra("message") ?: "No message"
            txtBroadcastMessage.text = "AnalyticsApp received: $message"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        txtBroadcastMessage = findViewById(R.id.txtBroadcastMessage)

        // Register the receiver (same action and permission)
        val filter = IntentFilter("com.example.ACTION_BUDGET_REMINDER")
        registerReceiver(budgetReceiver, filter, "com.example.permission.RECEIVE_BUDGET_REMINDER", null)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(budgetReceiver)
    }
}
