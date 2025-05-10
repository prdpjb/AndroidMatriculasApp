package com.example.platerecognitionapp.services

import android.content.Context
import com.example.platerecognitionapp.data.Plate
import com.example.platerecognitionapp.data.PlateDatabase
import com.example.platerecognitionapp.utils.ErrorLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

data class BusinessInsight {
    val totalPlatesRecognized: Int
    val uniquePlates: Int
    val recognitionFrequency: Map<LocalDateTime, Int>
    val topLocations: List<LocationFrequency>
    val timeBasedPatterns: List<TimePattern>
    val confidenceMetrics: ConfidenceAnalysis
}

data class LocationFrequency(
    val location: String,
    val frequency: Int
)

data class TimePattern(
    val timeSlot: String,
    val plateCount: Int,
    val peakHours: List<String>
)

data class ConfidenceAnalysis(
    val averageConfidence: Float,
    val confidenceBrackets: Map<String, Int>
)

class BusinessIntelligenceService(private val context: Context) {
    private val plateDatabase = PlateDatabase.getDatabase(context)
    private val errorLogger = ErrorLogger.getInstance(context)

    suspend fun generateBusinessInsights(
        startDate: LocalDateTime = LocalDateTime.now().minusMonths(1),
        endDate: LocalDateTime = LocalDateTime.now()
    ): BusinessInsight = withContext(Dispatchers.Default) {
        try {
            val allPlates = plateDatabase.plateDao()
                .getPlatesBetweenDates(startDate, endDate)
                .first()

            BusinessInsight(
                totalPlatesRecognized = allPlates.size,
                uniquePlates = allPlates.map { it.plateNumber }.distinct().size,
                recognitionFrequency = calculateRecognitionFrequency(allPlates),
                topLocations = findTopLocations(allPlates),
                timeBasedPatterns = analyzeTimePatterns(allPlates),
                confidenceMetrics = analyzeConfidence(allPlates)
            )
        } catch (e: Exception) {
            errorLogger.logError("Erro na geração de insights de negócios", e)
            throw e
        }
    }

    private fun calculateRecognitionFrequency(plates: List<Plate>): Map<LocalDateTime, Int> {
        return plates.groupBy { 
            it.capturedAt.truncatedTo(ChronoUnit.HOURS) 
        }.mapValues { it.value.size }
    }

    private fun findTopLocations(plates: List<Plate>): List<LocationFrequency> {
        return plates.groupBy { it.location }
            .mapValues { it.value.size }
            .entries
            .sortedByDescending { it.value }
            .take(5)
            .map { LocationFrequency(it.key, it.value) }
    }

    private fun analyzeTimePatterns(plates: List<Plate>): List<TimePattern> {
        val hourlyPatterns = plates.groupBy { 
            it.capturedAt.hour.toString().padStart(2, '0') + ":00" 
        }.mapValues { it.value.size }

        val peakHours = hourlyPatterns
            .entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key }

        return hourlyPatterns.map { (timeSlot, count) ->
            TimePattern(
                timeSlot = timeSlot,
                plateCount = count,
                peakHours = peakHours
            )
        }
    }

    private fun analyzeConfidence(plates: List<Plate>): ConfidenceAnalysis {
        val averageConfidence = plates.map { it.confidence }.average().toFloat()
        
        val confidenceBrackets = plates.groupBy { plate ->
            when {
                plate.confidence < 0.3 -> "Baixa Confiança"
                plate.confidence < 0.6 -> "Confiança Média"
                plate.confidence < 0.8 -> "Confiança Alta"
                else -> "Confiança Muito Alta"
            }
        }.mapValues { it.value.size }

        return ConfidenceAnalysis(
            averageConfidence = averageConfidence,
            confidenceBrackets = confidenceBrackets
        )
    }

    companion object {
        fun getInstance(context: Context): BusinessIntelligenceService {
            return BusinessIntelligenceService(context.applicationContext)
        }
    }
}
