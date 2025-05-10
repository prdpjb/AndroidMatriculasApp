package com.example.platerecognitionapp.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ErrorLogger(private val context: Context) {
    private val logFile: File by lazy {
        File(context.getExternalFilesDir(null), "plate_recognition_errors.log")
    }

    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val logMessage = "$timestamp - $tag: $message\n"
        
        // Log to Android LogCat
        Log.e(tag, message, throwable)
        
        // Log to file
        try {
            logFile.appendText(logMessage)
            throwable?.let { 
                logFile.appendText("${it.stackTraceToString()}\n")
            }
        } catch (e: Exception) {
            Log.e("ErrorLogger", "Failed to write to log file", e)
        }
    }

    fun shareLogFile() {
        // Implementar lógica de compartilhamento de arquivo de log
    }

    fun clearLogFile() {
        try {
            logFile.writeText("")
        } catch (e: Exception) {
            Log.e("ErrorLogger", "Failed to clear log file", e)
        }
    }

    companion object {
        fun getInstance(context: Context): ErrorLogger {
            return ErrorLogger(context.applicationContext)
        }
    }
}
