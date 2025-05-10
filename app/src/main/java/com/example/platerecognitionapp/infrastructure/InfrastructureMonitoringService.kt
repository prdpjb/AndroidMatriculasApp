package com.example.platerecognitionapp.infrastructure

import android.content.Context
import com.example.platerecognitionapp.utils.ErrorLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.time.LocalDateTime

data class SystemHealth(
    val overallStatus: SystemStatus,
    val serviceStatuses: Map<String, ServiceStatus>,
    val resourceUtilization: ResourceUtilization,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

enum class SystemStatus {
    HEALTHY,
    DEGRADED,
    CRITICAL
}

data class ServiceStatus(
    val name: String,
    val status: SystemStatus,
    val responseTime: Long,
    val errorRate: Float
)

data class ResourceUtilization(
    val cpuUsage: Float,
    val memoryUsage: Float,
    val diskUsage: Float,
    val networkTraffic: Long
)

data class AlertConfiguration(
    val cpuThreshold: Float = 80f,
    val memoryThreshold: Float = 85f,
    val diskThreshold: Float = 90f,
    val errorRateThreshold: Float = 5f
)

class InfrastructureMonitoringService(private val context: Context) {
    private val errorLogger = ErrorLogger.getInstance(context)
    private val httpClient = OkHttpClient()
    private val monitoringBaseUrl = "https://monitoring.platerecognition.com/api"
    private val alertConfiguration = AlertConfiguration()

    suspend fun getSystemHealth(): SystemHealth = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$monitoringBaseUrl/health")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                throw Exception("Falha ao obter saúde do sistema")
            }

            val responseBody = response.body()?.string() ?: "{}"
            val healthJson = JSONObject(responseBody)

            SystemHealth(
                overallStatus = SystemStatus.valueOf(
                    healthJson.getString("overallStatus").uppercase()
                ),
                serviceStatuses = parseServiceStatuses(
                    healthJson.getJSONObject("serviceStatuses")
                ),
                resourceUtilization = parseResourceUtilization(
                    healthJson.getJSONObject("resourceUtilization")
                )
            )
        } catch (e: Exception) {
            errorLogger.logError("Erro ao obter saúde do sistema", e)
            SystemHealth(
                overallStatus = SystemStatus.CRITICAL,
                serviceStatuses = emptyMap(),
                resourceUtilization = ResourceUtilization(
                    cpuUsage = 0f,
                    memoryUsage = 0f,
                    diskUsage = 0f,
                    networkTraffic = 0
                )
            )
        }
    }

    private fun parseServiceStatuses(
        serviceStatusesJson: JSONObject
    ): Map<String, ServiceStatus> {
        return serviceStatusesJson.keys().asSequence().associate { serviceName ->
            val serviceJson = serviceStatusesJson.getJSONObject(serviceName)
            serviceName to ServiceStatus(
                name = serviceName,
                status = SystemStatus.valueOf(
                    serviceJson.getString("status").uppercase()
                ),
                responseTime = serviceJson.getLong("responseTime"),
                errorRate = serviceJson.getDouble("errorRate").toFloat()
            )
        }
    }

    private fun parseResourceUtilization(
        resourceUtilizationJson: JSONObject
    ): ResourceUtilization {
        return ResourceUtilization(
            cpuUsage = resourceUtilizationJson.getDouble("cpuUsage").toFloat(),
            memoryUsage = resourceUtilizationJson.getDouble("memoryUsage").toFloat(),
            diskUsage = resourceUtilizationJson.getDouble("diskUsage").toFloat(),
            networkTraffic = resourceUtilizationJson.getLong("networkTraffic")
        )
    }

    suspend fun checkAndTriggerAlerts(systemHealth: SystemHealth): List<String> = withContext(Dispatchers.IO) {
        val alerts = mutableListOf<String>()

        // Verificar limites de recursos
        with(systemHealth.resourceUtilization) {
            if (cpuUsage > alertConfiguration.cpuThreshold) {
                alerts.add("ALERTA: Uso de CPU alto (${cpuUsage}%)")
            }
            if (memoryUsage > alertConfiguration.memoryThreshold) {
                alerts.add("ALERTA: Uso de Memória alto (${memoryUsage}%)")
            }
            if (diskUsage > alertConfiguration.diskThreshold) {
                alerts.add("ALERTA: Uso de Disco alto (${diskUsage}%)")
            }
        }

        // Verificar status de serviços
        systemHealth.serviceStatuses.forEach { (serviceName, serviceStatus) ->
            if (serviceStatus.status != SystemStatus.HEALTHY) {
                alerts.add("ALERTA: Serviço $serviceName com status ${serviceStatus.status}")
            }
            if (serviceStatus.errorRate > alertConfiguration.errorRateThreshold) {
                alerts.add("ALERTA: Taxa de erros alta para $serviceName (${serviceStatus.errorRate}%)")
            }
        }

        // Enviar alertas se necessário
        if (alerts.isNotEmpty()) {
            sendAlerts(alerts)
        }

        alerts
    }

    private suspend fun sendAlerts(alerts: List<String>) = withContext(Dispatchers.IO) {
        try {
            val requestBody = JSONObject().apply {
                put("alerts", alerts)
            }

            val request = Request.Builder()
                .url("$monitoringBaseUrl/alerts")
                .post(okhttp3.RequestBody.create(
                    okhttp3.MediaType.parse("application/json"), 
                    requestBody.toString()
                ))
                .build()

            val response = httpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                throw Exception("Falha ao enviar alertas")
            }

            errorLogger.logInfo("Alertas enviados: $alerts")
        } catch (e: Exception) {
            errorLogger.logError("Erro ao enviar alertas", e)
        }
    }

    companion object {
        private var instance: InfrastructureMonitoringService? = null

        fun getInstance(context: Context): InfrastructureMonitoringService {
            return instance ?: synchronized(this) {
                instance ?: InfrastructureMonitoringService(context.applicationContext).also { 
                    instance = it 
                }
            }
        }
    }
}
