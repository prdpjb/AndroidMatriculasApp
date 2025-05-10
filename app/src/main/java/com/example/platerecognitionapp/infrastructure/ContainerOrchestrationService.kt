package com.example.platerecognitionapp.infrastructure

import android.content.Context
import com.example.platerecognitionapp.utils.ErrorLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.time.LocalDateTime

enum class ContainerStatus {
    RUNNING,
    STOPPED,
    STARTING,
    ERROR
}

data class ContainerMetrics(
    val containerId: String,
    val status: ContainerStatus,
    val cpuUsage: Float,
    val memoryUsage: Float,
    val networkTraffic: Long,
    val uptime: Long,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

data class ScalingPolicy(
    val minInstances: Int = 1,
    val maxInstances: Int = 10,
    val cpuThresholdPercent: Float = 70f,
    val memoryThresholdPercent: Float = 80f
)

class ContainerOrchestrationService(private val context: Context) {
    private val errorLogger = ErrorLogger.getInstance(context)
    private val httpClient = OkHttpClient()
    private val orchestrationBaseUrl = "https://container-orchestrator.platerecognition.com/api"

    suspend fun deployContainer(
        serviceName: String,
        imageTag: String,
        scalingPolicy: ScalingPolicy = ScalingPolicy()
    ): String = withContext(Dispatchers.IO) {
        try {
            val requestBody = JSONObject().apply {
                put("serviceName", serviceName)
                put("imageTag", imageTag)
                put("scalingPolicy", JSONObject().apply {
                    put("minInstances", scalingPolicy.minInstances)
                    put("maxInstances", scalingPolicy.maxInstances)
                    put("cpuThreshold", scalingPolicy.cpuThresholdPercent)
                    put("memoryThreshold", scalingPolicy.memoryThresholdPercent)
                })
            }

            val request = Request.Builder()
                .url("$orchestrationBaseUrl/deploy")
                .post(okhttp3.RequestBody.create(
                    okhttp3.MediaType.parse("application/json"), 
                    requestBody.toString()
                ))
                .build()

            val response = httpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                throw Exception("Falha na implantação do contêiner")
            }

            val responseBody = response.body()?.string() ?: ""
            val containerId = JSONObject(responseBody).getString("containerId")
            
            errorLogger.logInfo("Contêiner implantado: $containerId")
            containerId
        } catch (e: Exception) {
            errorLogger.logError("Erro na implantação de contêiner", e)
            throw e
        }
    }

    suspend fun getContainerMetrics(containerId: String): ContainerMetrics = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$orchestrationBaseUrl/metrics/$containerId")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                throw Exception("Falha ao obter métricas do contêiner")
            }

            val responseBody = response.body()?.string() ?: "{}"
            val metricsJson = JSONObject(responseBody)

            ContainerMetrics(
                containerId = containerId,
                status = ContainerStatus.valueOf(
                    metricsJson.getString("status").uppercase()
                ),
                cpuUsage = metricsJson.getDouble("cpuUsage").toFloat(),
                memoryUsage = metricsJson.getDouble("memoryUsage").toFloat(),
                networkTraffic = metricsJson.getLong("networkTraffic"),
                uptime = metricsJson.getLong("uptime")
            )
        } catch (e: Exception) {
            errorLogger.logError("Erro ao obter métricas do contêiner", e)
            ContainerMetrics(
                containerId = containerId,
                status = ContainerStatus.ERROR,
                cpuUsage = 0f,
                memoryUsage = 0f,
                networkTraffic = 0,
                uptime = 0
            )
        }
    }

    suspend fun scaleService(
        serviceName: String, 
        desiredInstances: Int
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val requestBody = JSONObject().apply {
                put("serviceName", serviceName)
                put("desiredInstances", desiredInstances)
            }

            val request = Request.Builder()
                .url("$orchestrationBaseUrl/scale")
                .post(okhttp3.RequestBody.create(
                    okhttp3.MediaType.parse("application/json"), 
                    requestBody.toString()
                ))
                .build()

            val response = httpClient.newCall(request).execute()
            
            val isSuccessful = response.isSuccessful
            if (isSuccessful) {
                errorLogger.logInfo("Serviço $serviceName escalado para $desiredInstances instâncias")
            }

            isSuccessful
        } catch (e: Exception) {
            errorLogger.logError("Erro no escalonamento de serviço", e)
            false
        }
    }

    suspend fun autoScaleService(serviceName: String): Boolean {
        return try {
            val metrics = getContainerMetrics(serviceName)
            val shouldScale = when {
                metrics.cpuUsage > 80 -> scaleService(serviceName, metrics.cpuUsage.toInt() / 10)
                metrics.memoryUsage > 90 -> scaleService(serviceName, metrics.memoryUsage.toInt() / 10)
                else -> false
            }
            shouldScale
        } catch (e: Exception) {
            errorLogger.logError("Erro no autoescalonamento", e)
            false
        }
    }

    companion object {
        private var instance: ContainerOrchestrationService? = null

        fun getInstance(context: Context): ContainerOrchestrationService {
            return instance ?: synchronized(this) {
                instance ?: ContainerOrchestrationService(context.applicationContext).also { 
                    instance = it 
                }
            }
        }
    }
}
