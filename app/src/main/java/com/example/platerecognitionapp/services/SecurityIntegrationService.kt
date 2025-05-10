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

data class SecurityAlert(
    val plateNumber: String,
    val alertType: SecurityAlertType,
    val details: Map<String, String>,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

enum class SecurityAlertType {
    STOLEN_VEHICLE,
    WANTED_VEHICLE,
    BORDER_CROSSING,
    CRIMINAL_RECORD,
    INTERNATIONAL_WATCH_LIST
}

class SecurityIntegrationService(private val context: Context) {
    private val errorLogger = ErrorLogger.getInstance(context)
    private val httpClient = OkHttpClient()

    suspend fun checkSecurityStatus(plate: Plate): List<SecurityAlert> = withContext(Dispatchers.IO) {
        try {
            val internationalAlert = checkInternationalWatchList(plate)
            val nationalAlert = checkNationalSecurityDatabase(plate)
            val borderCrossingAlert = checkBorderCrossing(plate)

            listOfNotNull(internationalAlert, nationalAlert, borderCrossingAlert)
        } catch (e: Exception) {
            errorLogger.logError("Erro na verificação de segurança", e)
            emptyList()
        }
    }

    private suspend fun checkInternationalWatchList(plate: Plate): SecurityAlert? {
        val request = Request.Builder()
            .url("https://api.interpol.int/watchlist?plate=${plate.plateNumber}")
            .build()

        val response = httpClient.newCall(request).execute()
        
        return if (response.isSuccessful) {
            val responseBody = response.body?.string() ?: return null
            val jsonResponse = JSONObject(responseBody)

            if (jsonResponse.getBoolean("onWatchList")) {
                SecurityAlert(
                    plateNumber = plate.plateNumber,
                    alertType = SecurityAlertType.INTERNATIONAL_WATCH_LIST,
                    details = mapOf(
                        "watchListReason" to jsonResponse.getString("reason"),
                        "countryOfOrigin" to jsonResponse.getString("country")
                    )
                )
            } else null
        } else null
    }

    private suspend fun checkNationalSecurityDatabase(plate: Plate): SecurityAlert? {
        val request = Request.Builder()
            .url("https://api.securidadeveicular.gov.pt/status?plate=${plate.plateNumber}")
            .build()

        val response = httpClient.newCall(request).execute()
        
        return if (response.isSuccessful) {
            val responseBody = response.body?.string() ?: return null
            val jsonResponse = JSONObject(responseBody)

            when (jsonResponse.getString("status")) {
                "stolen" -> SecurityAlert(
                    plateNumber = plate.plateNumber,
                    alertType = SecurityAlertType.STOLEN_VEHICLE,
                    details = mapOf(
                        "reportDate" to jsonResponse.getString("reportDate"),
                        "reportLocation" to jsonResponse.getString("reportLocation")
                    )
                )
                "wanted" -> SecurityAlert(
                    plateNumber = plate.plateNumber,
                    alertType = SecurityAlertType.WANTED_VEHICLE,
                    details = mapOf(
                        "criminalRecord" to jsonResponse.getString("criminalRecord"),
                        "warrantDetails" to jsonResponse.getString("warrantDetails")
                    )
                )
                else -> null
            }
        } else null
    }

    private suspend fun checkBorderCrossing(plate: Plate): SecurityAlert? {
        val request = Request.Builder()
            .url("https://api.bordercontrol.eu/crossings?plate=${plate.plateNumber}")
            .build()

        val response = httpClient.newCall(request).execute()
        
        return if (response.isSuccessful) {
            val responseBody = response.body?.string() ?: return null
            val jsonResponse = JSONObject(responseBody)

            if (jsonResponse.getBoolean("recentCrossing")) {
                SecurityAlert(
                    plateNumber = plate.plateNumber,
                    alertType = SecurityAlertType.BORDER_CROSSING,
                    details = mapOf(
                        "crossingPoint" to jsonResponse.getString("crossingPoint"),
                        "crossingTime" to jsonResponse.getString("crossingTime"),
                        "direction" to jsonResponse.getString("direction")
                    )
                )
            } else null
        } else null
    }

    suspend fun reportSuspiciousVehicle(plate: Plate, location: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val reportData = JSONObject().apply {
                put("plate", plate.plateNumber)
                put("location", location)
                put("timestamp", LocalDateTime.now().toString())
            }

            val request = Request.Builder()
                .url("https://api.securidadeveicular.gov.pt/report-suspicious")
                .post(okhttp3.RequestBody.create(
                    okhttp3.MediaType.parse("application/json"), 
                    reportData.toString()
                ))
                .build()

            val response = httpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            errorLogger.logError("Erro ao reportar veículo suspeito", e)
            false
        }
    }

    companion object {
        fun getInstance(context: Context): SecurityIntegrationService {
            return SecurityIntegrationService(context.applicationContext)
        }
    }
}
