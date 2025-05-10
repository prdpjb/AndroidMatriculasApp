package com.example.platerecognitionapp.services

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.platerecognitionapp.data.PlateDatabase
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DataBackupService(private val context: Context) {
    private val plateDatabase = PlateDatabase.getDatabase(context)
    private val gson = Gson()

    suspend fun backupToFile(backupUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val plates = plateDatabase.plateDao().getAllPlates().first()
            
            context.contentResolver.openOutputStream(backupUri)?.use { outputStream ->
                BufferedWriter(OutputStreamWriter(outputStream)).use { writer ->
                    val backupData = BackupData(
                        timestamp = LocalDateTime.now(),
                        plates = plates
                    )
                    writer.write(gson.toJson(backupData))
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun restoreFromFile(backupUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(backupUri)
            val jsonString = inputStream?.bufferedReader()?.use { it.readText() }
            
            jsonString?.let { json ->
                val backupData = gson.fromJson(json, BackupData::class.java)
                
                // Limpar banco de dados existente
                plateDatabase.plateDao().clearAll()
                
                // Inserir matrículas do backup
                backupData.plates.forEach { plate ->
                    plateDatabase.plateDao().insert(plate)
                }
                true
            } ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun generateDefaultBackupFileName(): String {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        return "plate_backup_$timestamp.json"
    }

    data class BackupData(
        val timestamp: LocalDateTime,
        val plates: List<com.example.platerecognitionapp.data.Plate>
    )

    companion object {
        fun getInstance(context: Context): DataBackupService {
            return DataBackupService(context.applicationContext)
        }
    }
}
