package motloung.koena.analyticsapp

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AnalyticsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analytics)



        val summary = intent.getStringExtra("summary")
        findViewById<TextView>(R.id.summaryTextView).text = summary
    }
}
