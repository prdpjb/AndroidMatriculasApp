package com.example.platerecognitionapp.services

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.platerecognitionapp.data.Plate
import com.example.platerecognitionapp.utils.ErrorLogger
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ReportGenerationService(private val context: Context) {
    private val errorLogger = ErrorLogger.getInstance(context)
    private val businessIntelligenceService = BusinessIntelligenceService.getInstance(context)
    private val predictiveAnalyticsService = PredictiveAnalyticsService.getInstance(context)

    suspend fun generateComprehensiveReport(
        plates: List<Plate>,
        startDate: LocalDateTime = LocalDateTime.now().minusMonths(1),
        endDate: LocalDateTime = LocalDateTime.now()
    ): File = withContext(Dispatchers.Default) {
        try {
            val businessInsights = businessIntelligenceService.generateBusinessInsights(startDate, endDate)
            val predictiveInsights = plates.map { plate ->
                predictiveAnalyticsService.generatePredictiveInsights(plate)
            }

            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            drawReportHeader(canvas, startDate, endDate)
            drawBusinessInsightsSummary(canvas, businessInsights)
            drawConfidenceChart(canvas, businessInsights.confidenceMetrics)
            drawTimePatternChart(canvas, businessInsights.timeBasedPatterns)
            drawPredictiveInsightsSummary(canvas, predictiveInsights)

            pdfDocument.finishPage(page)

            val reportFile = File(context.getExternalFilesDir(null), "plate_recognition_report.pdf")
            val outputStream = FileOutputStream(reportFile)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.close()

            reportFile
        } catch (e: Exception) {
            errorLogger.logError("Erro na geração de relatório", e)
            throw e
        }
    }

    private fun drawReportHeader(
        canvas: Canvas, 
        startDate: LocalDateTime, 
        endDate: LocalDateTime
    ) {
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 16f
            isFakeBoldText = true
        }

        canvas.drawText("Relatório de Reconhecimento de Matrículas", 50f, 50f, paint)
        
        paint.textSize = 12f
        paint.isFakeBoldText = false
        
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        canvas.drawText(
            "Período: ${startDate.format(formatter)} - ${endDate.format(formatter)}", 
            50f, 
            70f, 
            paint
        )
    }

    private fun drawBusinessInsightsSummary(
        canvas: Canvas, 
        businessInsights: BusinessInsight
    ) {
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
        }

        val summaryText = """
            Total de Matrículas: ${businessInsights.totalPlatesRecognized}
            Matrículas Únicas: ${businessInsights.uniquePlates}
            Locais Mais Frequentes: ${businessInsights.topLocations.joinToString { it.location }}
        """.trimIndent()

        canvas.drawText(summaryText, 50f, 120f, paint)
    }

    private fun drawConfidenceChart(
        canvas: Canvas, 
        confidenceMetrics: ConfidenceAnalysis
    ) {
        val pieEntries = confidenceMetrics.confidenceBrackets.map { 
            PieEntry(it.value.toFloat(), it.key) 
        }

        val pieDataSet = PieDataSet(pieEntries, "Níveis de Confiança")
        pieDataSet.colors = listOf(
            Color.GREEN, Color.YELLOW, Color.BLUE, Color.RED
        )

        val pieData = PieData(pieDataSet)
        
        // Renderizar gráfico de pizza diretamente no canvas
        // (Implementação simplificada, considerar biblioteca de gráficos)
    }

    private fun drawTimePatternChart(
        canvas: Canvas, 
        timePatterns: List<TimePattern>
    ) {
        val barEntries = timePatterns.mapIndexed { index, pattern ->
            BarEntry(index.toFloat(), pattern.plateCount.toFloat())
        }

        val barDataSet = BarDataSet(barEntries, "Padrões de Tempo")
        barDataSet.color = Color.BLUE

        val barData = BarData(barDataSet)
        
        // Renderizar gráfico de barras diretamente no canvas
        // (Implementação simplificada, considerar biblioteca de gráficos)
    }

    private fun drawPredictiveInsightsSummary(
        canvas: Canvas, 
        predictiveInsights: List<PredictiveInsight>
    ) {
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
        }

        val highRiskPlates = predictiveInsights.filter { it.riskScore > 0.7 }
        val summaryText = """
            Matrículas de Alto Risco: ${highRiskPlates.size}
            Principais Eventos Previstos: ${
                predictiveInsights
                    .flatMap { it.predictedEvents }
                    .groupBy { it.eventType }
                    .mapValues { it.value.size }
            }
        """.trimIndent()

        canvas.drawText(summaryText, 50f, 300f, paint)
    }

    companion object {
        fun getInstance(context: Context): ReportGenerationService {
            return ReportGenerationService(context.applicationContext)
        }
    }
}
