package motloung.koena.analyticsapp

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AnalyticsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analytics)

        val trustedPackage = "motloung.koena.financeapp"
        if (callingActivity?.packageName != trustedPackage) {
            finish() // Block if not trusted
        }

        val summary = intent.getStringExtra("summary")
        findViewById<TextView>(R.id.summaryTextView).text = summary
    }
}
