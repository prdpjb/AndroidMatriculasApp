package com.example.platerecognitionapp.reporting

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.platerecognitionapp.analytics.TrendAnalysisService
import com.example.platerecognitionapp.analytics.TrendInsight
import com.example.platerecognitionapp.data.PlateDatabase
import com.example.platerecognitionapp.utils.ErrorLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class ReportConfiguration(
    val includeInsights: Boolean = true,
    val includeRawData: Boolean = false,
    val anonymizeData: Boolean = true,
    val timeRange: TimeRange = TimeRange.LAST_MONTH
)

enum class TimeRange {
    LAST_WEEK,
    LAST_MONTH,
    LAST_QUARTER,
    LAST_YEAR,
    ALL_TIME
}

enum class ReportFormat {
    PDF,
    CSV,
    JSON
}

class ReportGenerationService(private val context: Context) {
    private val plateDatabase = PlateDatabase.getDatabase(context)
    private val trendAnalysisService = TrendAnalysisService.getInstance(context)
    private val errorLogger = ErrorLogger.getInstance(context)

    suspend fun generateReport(
        configuration: ReportConfiguration = ReportConfiguration(),
        format: ReportFormat = ReportFormat.PDF
    ): File = withContext(Dispatchers.Default) {
        try {
            val insights = if (configuration.includeInsights) {
                trendAnalysisService.analyzeTrends()
            } else emptyList()

            val plates = plateDatabase.plateDao().getPlatesByTimeRange(
                when (configuration.timeRange) {
                    TimeRange.LAST_WEEK -> LocalDateTime.now().minusWeeks(1)
                    TimeRange.LAST_MONTH -> LocalDateTime.now().minusMonths(1)
                    TimeRange.LAST_QUARTER -> LocalDateTime.now().minusMonths(3)
                    TimeRange.LAST_YEAR -> LocalDateTime.now().minusYears(1)
                    TimeRange.ALL_TIME -> LocalDateTime.MIN
                }
            )

            when (format) {
                ReportFormat.PDF -> generatePdfReport(insights, plates, configuration)
                ReportFormat.CSV -> generateCsvReport(insights, plates, configuration)
                ReportFormat.JSON -> generateJsonReport(insights, plates, configuration)
            }
        } catch (e: Exception) {
            errorLogger.logError("Erro na geração de relatório", e)
            throw e
        }
    }

    private fun generatePdfReport(
        insights: List<TrendInsight>,
        plates: List<*>,
        configuration: ReportConfiguration
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        // Título do relatório
        paint.color = Color.BLACK
        paint.textSize = 18f
        canvas.drawText("Relatório de Placas", 50f, 50f, paint)

        // Detalhes do relatório
        paint.textSize = 12f
        canvas.drawText(
            "Gerado em: ${LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}",
            50f, 
            80f, 
            paint
        )

        // Insights
        var yOffset = 120f
        if (configuration.includeInsights) {
            paint.textSize = 14f
            canvas.drawText("Insights", 50f, yOffset, paint)
            yOffset += 30f

            insights.forEach { insight ->
                paint.textSize = 12f
                canvas.drawText(
                    "Tipo: ${insight.trendType}, Intensidade: ${insight.intensity}", 
                    70f, 
                    yOffset, 
                    paint
                )
                yOffset += 20f
            }
        }

        document.finishPage(page)

        // Salvar PDF
        val file = File(
            context.getExternalFilesDir("reports"), 
            "relatorio_placas_${System.currentTimeMillis()}.pdf"
        )
        document.writeTo(file.outputStream())
        document.close()

        return file
    }

    private fun generateCsvReport(
        insights: List<TrendInsight>,
        plates: List<*>,
        configuration: ReportConfiguration
    ): File {
        val file = File(
            context.getExternalFilesDir("reports"), 
            "relatorio_placas_${System.currentTimeMillis()}.csv"
        )

        file.printWriter().use { out ->
            // Cabeçalho
            out.println("Tipo de Insight,Intensidade,Data Inicial,Data Final,Placas Afetadas")
            
            // Dados de insights
            insights.forEach { insight ->
                out.println(
                    "${insight.trendType}," +
                    "${insight.intensity}," +
                    "${insight.startDate}," +
                    "${insight.endDate}," +
                    "${insight.affectedPlates}"
                )
            }
        }

        return file
    }

    private fun generateJsonReport(
        insights: List<TrendInsight>,
        plates: List<*>,
        configuration: ReportConfiguration
    ): File {
        val file = File(
            context.getExternalFilesDir("reports"), 
            "relatorio_placas_${System.currentTimeMillis()}.json"
        )

        val jsonContent = """
        {
            "metadata": {
                "generatedAt": "${LocalDateTime.now()}",
                "timeRange": "${configuration.timeRange}"
            },
            "insights": ${insights.map { it.toString() }},
            "plateCount": ${plates.size}
        }
        """.trimIndent()

        file.writeText(jsonContent)

        return file
    }

    companion object {
        private var instance: ReportGenerationService? = null

        fun getInstance(context: Context): ReportGenerationService {
            return instance ?: synchronized(this) {
                instance ?: ReportGenerationService(context.applicationContext).also { 
                    instance = it 
                }
            }
        }
    }
}
