package com.example.platerecognitionapp.services

import android.content.Context
import com.example.platerecognitionapp.data.Plate
import com.example.platerecognitionapp.utils.ErrorLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.time.LocalDateTime

class TrafficIntegrationService(private val context: Context) {
    private val errorLogger = ErrorLogger.getInstance(context)
    private val httpClient = OkHttpClient()

    suspend fun checkVehicleStatus(plate: Plate): VehicleStatus = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.trafficregistry.gov.pt/vehicle-status?plate=${plate.plateNumber}")
                .build()

            val response = httpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return@withContext VehicleStatus.UNKNOWN
            }

            val responseBody = response.body?.string() ?: return@withContext VehicleStatus.UNKNOWN
            val jsonResponse = JSONObject(responseBody)

            when (jsonResponse.getString("status")) {
                "stolen" -> VehicleStatus.STOLEN
                "wanted" -> VehicleStatus.WANTED
                "expired_insurance" -> VehicleStatus.EXPIRED_INSURANCE
                "expired_inspection" -> VehicleStatus.EXPIRED_INSPECTION
                else -> VehicleStatus.CLEAR
            }
        } catch (e: Exception) {
            errorLogger.logError("Erro na verificação de status do veículo", e)
            VehicleStatus.UNKNOWN
        }
    }

    suspend fun reportUnusualActivity(plate: Plate, location: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val reportData = JSONObject().apply {
                put("plate", plate.plateNumber)
                put("location", location)
                put("timestamp", LocalDateTime.now().toString())
            }

            val request = Request.Builder()
                .url("https://api.trafficregistry.gov.pt/unusual-activity")
                .post(okhttp3.RequestBody.create(
                    okhttp3.MediaType.parse("application/json"), 
                    reportData.toString()
                ))
                .build()

            val response = httpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            errorLogger.logError("Erro ao reportar atividade incomum", e)
            false
        }
    }

    enum class VehicleStatus {
        CLEAR,
        STOLEN,
        WANTED,
        EXPIRED_INSURANCE,
        EXPIRED_INSPECTION,
        UNKNOWN
    }

    companion object {
        fun getInstance(context: Context): TrafficIntegrationService {
            return TrafficIntegrationService(context.applicationContext)
        }
    }
}
