package com.example.platerecognitionapp.utils

import android.content.Context
import android.os.Environment
import com.example.platerecognitionapp.data.Plate
import kotlinx.coroutines.flow.first
import java.io.File
import java.time.format.DateTimeFormatter

class PlateExporter(private val context: Context) {
    private val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")

    suspend fun exportToCSV(plates: List<Plate>): File {
        val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val file = File(directory, "plate_history_${System.currentTimeMillis()}.csv")

        file.printWriter().use { out ->
            out.println("ID,Matrícula,Data de Captura")
            plates.forEach { plate ->
                out.println("${plate.id},${plate.plateNumber},${plate.capturedAt.format(formatter)}")
            }
        }

        return file
    }

    suspend fun shareCSV(plates: List<Plate>) {
        val file = exportToCSV(plates)
        // Implementar lógica de compartilhamento de arquivo
    }
}
