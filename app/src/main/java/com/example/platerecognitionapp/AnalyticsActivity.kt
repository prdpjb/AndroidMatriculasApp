package com.example.platerecognitionapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.platerecognitionapp.databinding.ActivityAnalyticsBinding
import com.example.platerecognitionapp.services.PlateAnalyticsService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AnalyticsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAnalyticsBinding
    private lateinit var plateAnalyticsService: PlateAnalyticsService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnalyticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        plateAnalyticsService = PlateAnalyticsService(this)

        loadAnalytics()

        binding.shareReportButton.setOnClickListener {
            shareAnalyticsReport()
        }
    }

    private fun loadAnalytics() {
        lifecycleScope.launch {
            val totalPlates = withContext(Dispatchers.IO) { 
                plateAnalyticsService.getTotalPlatesCaptured() 
            }
            val todayPlates = withContext(Dispatchers.IO) { 
                plateAnalyticsService.getPlatesCapturedToday() 
            }
            val mostFrequentPlates = withContext(Dispatchers.IO) { 
                plateAnalyticsService.getMostFrequentPlates() 
            }

            binding.totalPlatesTextView.text = "Total de Matrículas: $totalPlates"
            binding.todayPlatesTextView.text = "Matrículas Hoje: $todayPlates"
            
            binding.mostFrequentPlatesTextView.text = "Top Matrículas:\n" + 
                mostFrequentPlates.joinToString("\n") { (plate, count) -> 
                    "$plate: $count vezes" 
                }
        }
    }

    private fun shareAnalyticsReport() {
        lifecycleScope.launch {
            val report = withContext(Dispatchers.IO) { 
                plateAnalyticsService.generateAnalyticsReport() 
            }
            
            // Implementar lógica de compartilhamento
            val shareIntent = android.content.Intent().apply {
                action = android.content.Intent.ACTION_SEND
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_TEXT, report)
            }
            startActivity(android.content.Intent.createChooser(shareIntent, "Compartilhar Relatório"))
        }
    }
}
