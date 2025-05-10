package com.example.platerecognitionapp.analytics

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.example.platerecognitionapp.utils.ErrorLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime

data class InsightVisualization(
    val type: VisualizationType,
    val bitmap: Bitmap,
    val description: String,
    val generatedAt: LocalDateTime = LocalDateTime.now()
)

enum class VisualizationType {
    GEOGRAPHIC_HEATMAP,
    TEMPORAL_DISTRIBUTION,
    RISK_INTENSITY_MAP,
    TREND_PROGRESSION,
    ANOMALY_SCATTER
}

class InsightVisualizationService(private val context: Context) {
    private val errorLogger = ErrorLogger.getInstance(context)

    suspend fun generateVisualization(
        trendInsight: TrendInsight,
        width: Int = 1024,
        height: Int = 768
    ): InsightVisualization = withContext(Dispatchers.Default) {
        try {
            when (trendInsight.trendType) {
                TrendType.LOCATION_HOTSPOT -> createGeographicHeatmap(trendInsight, width, height)
                TrendType.TEMPORAL_PATTERN -> createTemporalDistribution(trendInsight, width, height)
                TrendType.SECURITY_RISK -> createRiskIntensityMap(trendInsight, width, height)
                TrendType.ANOMALY_CLUSTER -> createAnomalyScatterPlot(trendInsight, width, height)
                else -> createDefaultVisualization(trendInsight, width, height)
            }
        } catch (e: Exception) {
            errorLogger.logError("Erro na geração de visualização", e)
            createDefaultVisualization(trendInsight, width, height)
        }
    }

    private fun createGeographicHeatmap(
        trendInsight: TrendInsight,
        width: Int,
        height: Int
    ): InsightVisualization {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        // Recuperar hotspots do insight
        @Suppress("UNCHECKED_CAST")
        val hotspots = trendInsight.details["hotspots"] as List<GeographicHotspot>

        // Desenhar mapa de calor
        hotspots.forEachIndexed { index, hotspot ->
            paint.color = getHeatmapColor(hotspot.plateFrequency)
            val rect = RectF(
                index * (width / hotspots.size).toFloat(),
                0f,
                (index + 1) * (width / hotspots.size).toFloat(),
                height.toFloat()
            )
            canvas.drawRect(rect, paint)
        }

        return InsightVisualization(
            type = VisualizationType.GEOGRAPHIC_HEATMAP,
            bitmap = bitmap,
            description = "Mapa de Calor de Localidades"
        )
    }

    private fun createTemporalDistribution(
        trendInsight: TrendInsight,
        width: Int,
        height: Int
    ): InsightVisualization {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        // Recuperar padrões temporais do insight
        @Suppress("UNCHECKED_CAST")
        val temporalPatterns = trendInsight.details["temporalPatterns"] as List<TemporalPattern>

        // Desenhar distribuição temporal
        temporalPatterns.forEachIndexed { index, pattern ->
            paint.color = getTemporalColor(pattern.plateCount)
            val rect = RectF(
                index * (width / temporalPatterns.size).toFloat(),
                height - (pattern.plateCount * height / 100f),
                (index + 1) * (width / temporalPatterns.size).toFloat(),
                height.toFloat()
            )
            canvas.drawRect(rect, paint)
        }

        return InsightVisualization(
            type = VisualizationType.TEMPORAL_DISTRIBUTION,
            bitmap = bitmap,
            description = "Distribuição Temporal de Placas"
        )
    }

    private fun createRiskIntensityMap(
        trendInsight: TrendInsight,
        width: Int,
        height: Int
    ): InsightVisualization {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        // Recuperar placas suspeitas do insight
        @Suppress("UNCHECKED_CAST")
        val suspiciousPlates = trendInsight.details["suspiciousPlates"] as List<*>

        // Desenhar mapa de intensidade de risco
        suspiciousPlates.forEachIndexed { index, _ ->
            paint.color = getRiskColor(trendInsight.intensity)
            val rect = RectF(
                index * (width / suspiciousPlates.size).toFloat(),
                0f,
                (index + 1) * (width / suspiciousPlates.size).toFloat(),
                height.toFloat()
            )
            canvas.drawRect(rect, paint)
        }

        return InsightVisualization(
            type = VisualizationType.RISK_INTENSITY_MAP,
            bitmap = bitmap,
            description = "Mapa de Intensidade de Risco"
        )
    }

    private fun createAnomalyScatterPlot(
        trendInsight: TrendInsight,
        width: Int,
        height: Int
    ): InsightVisualization {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        // Recuperar placas anômalas do insight
        @Suppress("UNCHECKED_CAST")
        val anomalyPlates = trendInsight.details["anomalyPlates"] as List<*>

        // Desenhar gráfico de dispersão de anomalias
        anomalyPlates.forEachIndexed { index, _ ->
            paint.color = Color.RED
            canvas.drawCircle(
                index * (width / anomalyPlates.size).toFloat(),
                height / 2f,
                10f,
                paint
            )
        }

        return InsightVisualization(
            type = VisualizationType.ANOMALY_SCATTER,
            bitmap = bitmap,
            description = "Gráfico de Dispersão de Anomalias"
        )
    }

    private fun createDefaultVisualization(
        trendInsight: TrendInsight,
        width: Int,
        height: Int
    ): InsightVisualization {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        paint.color = Color.GRAY
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        return InsightVisualization(
            type = VisualizationType.TREND_PROGRESSION,
            bitmap = bitmap,
            description = "Visualização Padrão de Tendências"
        )
    }

    suspend fun saveVisualization(
        visualization: InsightVisualization,
        directory: File = context.getExternalFilesDir("insights")!!
    ): File = withContext(Dispatchers.IO) {
        val fileName = "insight_${visualization.type}_${System.currentTimeMillis()}.png"
        val file = File(directory, fileName)

        try {
            file.outputStream().use { out ->
                visualization.bitmap.compress(
                    Bitmap.CompressFormat.PNG, 
                    100, 
                    out
                )
            }
            errorLogger.logInfo("Visualização salva: $fileName")
            file
        } catch (e: Exception) {
            errorLogger.logError("Erro ao salvar visualização", e)
            throw e
        }
    }

    private fun getHeatmapColor(frequency: Int): Int {
        return when {
            frequency > 80 -> Color.RED
            frequency > 50 -> Color.YELLOW
            frequency > 20 -> Color.GREEN
            else -> Color.BLUE
        }
    }

    private fun getTemporalColor(count: Int): Int {
        return when {
            count > 80 -> Color.DKGRAY
            count > 50 -> Color.GRAY
            count > 20 -> Color.LTGRAY
            else -> Color.WHITE
        }
    }

    private fun getRiskColor(intensity: TrendIntensity): Int {
        return when (intensity) {
            TrendIntensity.CRITICAL -> Color.RED
            TrendIntensity.HIGH -> Color.YELLOW
            TrendIntensity.MODERATE -> Color.GREEN
            TrendIntensity.LOW -> Color.BLUE
        }
    }

    companion object {
        private var instance: InsightVisualizationService? = null

        fun getInstance(context: Context): InsightVisualizationService {
            return instance ?: synchronized(this) {
                instance ?: InsightVisualizationService(context.applicationContext).also { 
                    instance = it 
                }
            }
        }
    }
}
