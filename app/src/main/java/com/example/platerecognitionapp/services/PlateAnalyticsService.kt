package com.example.platerecognitionapp.services

import android.content.Context
import com.example.platerecognitionapp.data.PlateDatabase
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime

class PlateAnalyticsService(private val context: Context) {
    private val plateDatabase = PlateDatabase.getDatabase(context)

    suspend fun getTotalPlatesCaptured(): Int {
        return plateDatabase.plateDao().getAllPlates().first().size
    }

    suspend fun getPlatesCapturedToday(): Int {
        val today = LocalDate.now()
        return plateDatabase.plateDao().getAllPlates().first()
            .count { LocalDate.from(it.capturedAt) == today }
    }

    suspend fun getMostFrequentPlates(limit: Int = 5): List<Pair<String, Int>> {
        return plateDatabase.plateDao().getAllPlates().first()
            .groupingBy { it.plateNumber }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(limit)
    }

    suspend fun getPlatesCapturedByHour(): Map<Int, Int> {
        return plateDatabase.plateDao().getAllPlates().first()
            .groupBy { it.capturedAt.hour }
            .mapValues { it.value.size }
    }

    suspend fun generateAnalyticsReport(): String {
        val totalPlates = getTotalPlatesCaptured()
        val todayPlates = getPlatesCapturedToday()
        val mostFrequentPlates = getMostFrequentPlates()
        
        return """
            Relatório de Análise de Matrículas
            --------------------------------
            Total de Matrículas Capturadas: $totalPlates
            Matrículas Capturadas Hoje: $todayPlates
            
            Top 5 Matrículas Mais Frequentes:
            ${mostFrequentPlates.joinToString("\n") { (plate, count) -> "$plate: $count vezes" }}
        """.trimIndent()
    }
}
