package motloung.koena.analyticsapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import motloung.koena.analyticsapp.databinding.ActivityAnalyticsBinding

class AnalyticsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAnalyticsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnalyticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val summary = intent.getStringExtra("summary")
        binding.summaryTextView.text =
            "This is the Analytics App you have opened.\n\n\n$summary"
    }
}
