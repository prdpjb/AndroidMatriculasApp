package com.example.platerecognitionapp.services

import android.content.Context
import com.example.platerecognitionapp.data.Plate
import com.example.platerecognitionapp.data.PlateDatabase
import com.example.platerecognitionapp.utils.ErrorLogger
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.time.LocalDateTime

data class DataSharingConfig(
    val enableSharing: Boolean = false,
    val sharingFrequency: SharingFrequency = SharingFrequency.DAILY,
    val dataTypes: List<DataType> = listOf(DataType.PLATE_RECOGNITION)
)

enum class SharingFrequency {
    REAL_TIME,
    HOURLY,
    DAILY,
    WEEKLY,
    MONTHLY
}

enum class DataType {
    PLATE_RECOGNITION,
    ROUTE_ANALYSIS,
    ANOMALY_DETECTION,
    PREDICTIVE_INSIGHTS,
    AUDIT_LOGS
}

class DataSharingService(private val context: Context) {
    private val plateDatabase = PlateDatabase.getDatabase(context)
    private val errorLogger = ErrorLogger.getInstance(context)
    private val httpClient = OkHttpClient()
    private val gson = Gson()

    suspend fun shareData(
        config: DataSharingConfig = DataSharingConfig()
    ): Boolean = withContext(Dispatchers.IO) {
        if (!config.enableSharing) return@withContext false

        try {
            val dataToShare = collectDataForSharing(config.dataTypes)
            val dataFile = createDataFile(dataToShare)
            uploadDataToServer(dataFile)
        } catch (e: Exception) {
            errorLogger.logError("Erro no compartilhamento de dados", e)
            false
        }
    }

    private suspend fun collectDataForSharing(dataTypes: List<DataType>): Map<DataType, Any> {
        val sharedData = mutableMapOf<DataType, Any>()

        dataTypes.forEach { dataType ->
            when (dataType) {
                DataType.PLATE_RECOGNITION -> {
                    val plates = plateDatabase.plateDao()
                        .getPlatesBetweenDates(
                            LocalDateTime.now().minusDays(7),
                            LocalDateTime.now()
                        ).first()
                    sharedData[DataType.PLATE_RECOGNITION] = plates
                }
                DataType.ROUTE_ANALYSIS -> {
                    val routeAnalysisService = RouteAnalysisService.getInstance(context)
                    val plateRoutes = plateDatabase.plateDao()
                        .getAllPlates()
                        .first()
                        .map { plate -> 
                            routeAnalysisService.analyzeRouteForPlate(plate) 
                        }
                    sharedData[DataType.ROUTE_ANALYSIS] = plateRoutes
                }
                DataType.ANOMALY_DETECTION -> {
                    val anomalyDetectionService = AnomalyDetectionService.getInstance(context)
                    val plateAnomalies = plateDatabase.plateDao()
                        .getAllPlates()
                        .first()
                        .map { plate -> 
                            anomalyDetectionService.detectAnomalies(plate) 
                        }
                    sharedData[DataType.ANOMALY_DETECTION] = plateAnomalies
                }
                DataType.PREDICTIVE_INSIGHTS -> {
                    val predictiveAnalyticsService = PredictiveAnalyticsService.getInstance(context)
                    val predictiveInsights = plateDatabase.plateDao()
                        .getAllPlates()
                        .first()
                        .map { plate -> 
                            predictiveAnalyticsService.generatePredictiveInsights(plate) 
                        }
                    sharedData[DataType.PREDICTIVE_INSIGHTS] = predictiveInsights
                }
                DataType.AUDIT_LOGS -> {
                    val auditLogService = AuditLogService.getInstance(context)
                    val auditLogs = auditLogService.getRecentLogs(100)
                    sharedData[DataType.AUDIT_LOGS] = auditLogs
                }
            }
        }

        return sharedData
    }

    private fun createDataFile(data: Map<DataType, Any>): File {
        val jsonData = gson.toJson(data)
        val file = File(context.cacheDir, "shared_data_${System.currentTimeMillis()}.json")
        file.writeText(jsonData)
        return file
    }

    private fun uploadDataToServer(dataFile: File): Boolean {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "data", 
                dataFile.name, 
                dataFile.asRequestBody("application/json".toMediaTypeOrNull())
            )
            .build()

        val request = Request.Builder()
            .url("https://api.platerecognition.com/data-sharing")
            .post(requestBody)
            .build()

        val response = httpClient.newCall(request).execute()
        return response.isSuccessful
    }

    companion object {
        fun getInstance(context: Context): DataSharingService {
            return DataSharingService(context.applicationContext)
        }
    }
}
