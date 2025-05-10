package com.example.platerecognitionapp.analytics

import android.content.Context
import com.example.platerecognitionapp.data.Plate
import com.example.platerecognitionapp.data.PlateDatabase
import com.example.platerecognitionapp.utils.ErrorLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

data class TrendInsight(
    val trendType: TrendType,
    val intensity: TrendIntensity,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val affectedPlates: Int,
    val details: Map<String, Any>
)

enum class TrendType {
    VEHICLE_MOVEMENT,
    LOCATION_HOTSPOT,
    TEMPORAL_PATTERN,
    ANOMALY_CLUSTER,
    SECURITY_RISK
}

enum class TrendIntensity {
    LOW,
    MODERATE,
    HIGH,
    CRITICAL
}

data class GeographicHotspot(
    val location: String,
    val plateFrequency: Int,
    val uniquePlates: Int,
    val averageConfidence: Float
)

data class TemporalPattern(
    val timeSlot: String,
    val plateCount: Int,
    val peakHours: List<String>
)

class TrendAnalysisService(private val context: Context) {
    private val plateDatabase = PlateDatabase.getDatabase(context)
    private val errorLogger = ErrorLogger.getInstance(context)

    suspend fun analyzeTrends(
        startDate: LocalDateTime = LocalDateTime.now().minusMonths(1),
        endDate: LocalDateTime = LocalDateTime.now()
    ): List<TrendInsight> = withContext(Dispatchers.Default) {
        try {
            val plates = plateDatabase.plateDao()
                .getPlatesBetweenDates(startDate, endDate)
                .first()

            listOf(
                identifyGeographicHotspots(plates),
                detectTemporalPatterns(plates),
                findAnomalyCluster(plates),
                assessSecurityRisks(plates)
            )
        } catch (e: Exception) {
            errorLogger.logError("Erro na análise de tendências", e)
            emptyList()
        }
    }

    private fun identifyGeographicHotspots(plates: List<Plate>): TrendInsight {
        val locationFrequency = plates.groupBy { it.location }
            .mapValues { (_, locationPlates) -> 
                GeographicHotspot(
                    location = locationPlates.first().location,
                    plateFrequency = locationPlates.size,
                    uniquePlates = locationPlates.map { it.plateNumber }.distinct().size,
                    averageConfidence = locationPlates.map { it.confidence }.average().toFloat()
                )
            }

        val topHotspots = locationFrequency.values
            .sortedByDescending { it.plateFrequency }
            .take(5)

        return TrendInsight(
            trendType = TrendType.LOCATION_HOTSPOT,
            intensity = determineTrendIntensity(topHotspots.size),
            startDate = plates.minByOrNull { it.capturedAt }?.capturedAt ?: LocalDateTime.now(),
            endDate = plates.maxByOrNull { it.capturedAt }?.capturedAt ?: LocalDateTime.now(),
            affectedPlates = plates.size,
            details = mapOf("hotspots" to topHotspots)
        )
    }

    private fun detectTemporalPatterns(plates: List<Plate>): TrendInsight {
        val hourlyPatterns = plates.groupBy { 
            it.capturedAt.hour.toString().padStart(2, '0') + ":00" 
        }.mapValues { (_, hourPlates) -> 
            TemporalPattern(
                timeSlot = it.key,
                plateCount = hourPlates.size,
                peakHours = emptyList() // Implementação simplificada
            )
        }

        val sortedPatterns = hourlyPatterns.values
            .sortedByDescending { it.plateCount }

        return TrendInsight(
            trendType = TrendType.TEMPORAL_PATTERN,
            intensity = determineTrendIntensity(sortedPatterns.size),
            startDate = plates.minByOrNull { it.capturedAt }?.capturedAt ?: LocalDateTime.now(),
            endDate = plates.maxByOrNull { it.capturedAt }?.capturedAt ?: LocalDateTime.now(),
            affectedPlates = plates.size,
            details = mapOf("temporalPatterns" to sortedPatterns)
        )
    }

    private fun findAnomalyCluster(plates: List<Plate>): TrendInsight {
        val anomalyPlates = plates.filter { it.confidence < 0.5 }

        return TrendInsight(
            trendType = TrendType.ANOMALY_CLUSTER,
            intensity = determineTrendIntensity(anomalyPlates.size),
            startDate = plates.minByOrNull { it.capturedAt }?.capturedAt ?: LocalDateTime.now(),
            endDate = plates.maxByOrNull { it.capturedAt }?.capturedAt ?: LocalDateTime.now(),
            affectedPlates = anomalyPlates.size,
            details = mapOf("anomalyPlates" to anomalyPlates)
        )
    }

    private fun assessSecurityRisks(plates: List<Plate>): TrendInsight {
        val suspiciousPlates = plates.filter { plate ->
            // Critérios de risco de segurança
            plate.confidence < 0.6 || 
            plate.plateNumber.contains("STOLEN") || 
            plate.location.contains("HIGH_RISK_AREA")
        }

        return TrendInsight(
            trendType = TrendType.SECURITY_RISK,
            intensity = determineTrendIntensity(suspiciousPlates.size),
            startDate = plates.minByOrNull { it.capturedAt }?.capturedAt ?: LocalDateTime.now(),
            endDate = plates.maxByOrNull { it.capturedAt }?.capturedAt ?: LocalDateTime.now(),
            affectedPlates = suspiciousPlates.size,
            details = mapOf("suspiciousPlates" to suspiciousPlates)
        )
    }

    private fun determineTrendIntensity(count: Int): TrendIntensity {
        return when {
            count > 100 -> TrendIntensity.CRITICAL
            count > 50 -> TrendIntensity.HIGH
            count > 20 -> TrendIntensity.MODERATE
            else -> TrendIntensity.LOW
        }
    }

    companion object {
        private var instance: TrendAnalysisService? = null

        fun getInstance(context: Context): TrendAnalysisService {
            return instance ?: synchronized(this) {
                instance ?: TrendAnalysisService(context.applicationContext).also { 
                    instance = it 
                }
            }
        }
    }
}
