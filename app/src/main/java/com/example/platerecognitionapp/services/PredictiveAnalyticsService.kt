package com.example.platerecognitionapp.services

import android.content.Context
import com.example.platerecognitionapp.data.Plate
import com.example.platerecognitionapp.data.PlateDatabase
import com.example.platerecognitionapp.utils.ErrorLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.LocalDateTime
import kotlin.math.max
import kotlin.math.min

data class PredictiveInsight(
    val plateNumber: String,
    val riskScore: Float,
    val predictedEvents: List<PredictedEvent>,
    val recommendedActions: List<String>
)

data class PredictedEvent(
    val eventType: PredictedEventType,
    val probability: Float,
    val estimatedTime: LocalDateTime? = null
)

enum class PredictedEventType {
    THEFT_RISK,
    MAINTENANCE_NEEDED,
    UNUSUAL_MOVEMENT,
    POTENTIAL_VIOLATION,
    HIGH_MILEAGE
}

class PredictiveAnalyticsService(private val context: Context) {
    private val plateDatabase = PlateDatabase.getDatabase(context)
    private val routeAnalysisService = RouteAnalysisService.getInstance(context)
    private val anomalyDetectionService = AnomalyDetectionService.getInstance(context)
    private val errorLogger = ErrorLogger.getInstance(context)

    private val mlModel: Interpreter by lazy {
        try {
            val modelBuffer = loadModelFile()
            Interpreter(modelBuffer)
        } catch (e: Exception) {
            errorLogger.logError("Erro ao carregar modelo de ML", e)
            throw e
        }
    }

    private fun loadModelFile(): ByteBuffer {
        val assetManager = context.assets
        val inputStream = assetManager.open("predictive_model.tflite")
        val modelBuffer = ByteBuffer.allocateDirect(inputStream.available())
        modelBuffer.order(ByteOrder.nativeOrder())
        inputStream.read(modelBuffer.array())
        return modelBuffer
    }

    suspend fun generatePredictiveInsights(plate: Plate): PredictiveInsight = withContext(Dispatchers.Default) {
        try {
            val routeAnalysis = routeAnalysisService.analyzeRouteForPlate(plate)
            val anomalies = anomalyDetectionService.detectAnomalies(plate)

            val plateLocations = plateDatabase.plateLocationDao()
                .getLocationsByPlate(plate.id)
                .first()

            val inputFeatures = prepareInputFeatures(plate, routeAnalysis, anomalies, plateLocations)
            val riskScore = predictRiskScore(inputFeatures)
            val predictedEvents = predictPotentialEvents(inputFeatures)
            val recommendedActions = generateRecommendedActions(riskScore, predictedEvents)

            PredictiveInsight(
                plateNumber = plate.plateNumber,
                riskScore = riskScore,
                predictedEvents = predictedEvents,
                recommendedActions = recommendedActions
            )
        } catch (e: Exception) {
            errorLogger.logError("Erro na geração de insights preditivos", e)
            PredictiveInsight(
                plateNumber = plate.plateNumber,
                riskScore = 0f,
                predictedEvents = emptyList(),
                recommendedActions = emptyList()
            )
        }
    }

    private fun prepareInputFeatures(
        plate: Plate,
        routeAnalysis: RouteAnalysis,
        anomalies: List<AnomalyReport>,
        plateLocations: List<PlateLocation>
    ): FloatArray {
        return floatArrayOf(
            // Características de rota
            routeAnalysis.totalDistance,
            routeAnalysis.averageSpeed,
            routeAnalysis.routeVariation,

            // Características de anomalias
            anomalies.size.toFloat(),
            anomalies.count { it.severity == AnomalySeverity.HIGH }.toFloat(),

            // Características de localização
            plateLocations.size.toFloat(),
            plateLocations.maxByOrNull { it.capturedAt }?.let { 
                LocalDateTime.now().toEpochSecond(java.time.ZoneOffset.UTC) - 
                it.capturedAt.toEpochSecond(java.time.ZoneOffset.UTC) 
            }?.toFloat() ?: 0f,

            // Características do veículo
            plate.confidence.toFloat()
        )
    }

    private fun predictRiskScore(inputFeatures: FloatArray): Float {
        val outputBuffer = ByteBuffer.allocateDirect(4).apply { 
            order(ByteOrder.nativeOrder()) 
        }
        
        mlModel.run(inputFeatures, outputBuffer)
        
        return outputBuffer.getFloat(0).let { 
            max(0f, min(1f, it)) // Normalizar entre 0 e 1
        }
    }

    private fun predictPotentialEvents(inputFeatures: FloatArray): List<PredictedEvent> {
        val eventProbabilities = FloatArray(PredictedEventType.values().size)
        
        // Simular predição de eventos (substituir por modelo real de ML)
        eventProbabilities[PredictedEventType.THEFT_RISK.ordinal] = 
            inputFeatures[1] * 0.5f + inputFeatures[2] * 0.3f
        
        eventProbabilities[PredictedEventType.MAINTENANCE_NEEDED.ordinal] = 
            inputFeatures[0] * 0.4f + inputFeatures[7] * 0.6f
        
        eventProbabilities[PredictedEventType.UNUSUAL_MOVEMENT.ordinal] = 
            inputFeatures[2] * 0.7f + inputFeatures[3] * 0.3f
        
        return eventProbabilities.mapIndexed { index, probability ->
            if (probability > 0.5f) {
                PredictedEvent(
                    eventType = PredictedEventType.values()[index],
                    probability = probability,
                    estimatedTime = LocalDateTime.now().plusDays((probability * 30).toLong())
                )
            } else null
        }.filterNotNull()
    }

    private fun generateRecommendedActions(
        riskScore: Float, 
        predictedEvents: List<PredictedEvent>
    ): List<String> {
        val actions = mutableListOf<String>()

        if (riskScore > 0.7f) {
            actions.add("Realizar verificação de segurança completa")
        }

        predictedEvents.forEach { event ->
            when (event.eventType) {
                PredictedEventType.THEFT_RISK -> 
                    actions.add("Ativar sistema de rastreamento e alertar autoridades")
                
                PredictedEventType.MAINTENANCE_NEEDED -> 
                    actions.add("Agendar manutenção preventiva")
                
                PredictedEventType.UNUSUAL_MOVEMENT -> 
                    actions.add("Investigar padrões de movimentação suspeitos")
                
                PredictedEventType.POTENTIAL_VIOLATION -> 
                    actions.add("Verificar possíveis infrações de trânsito")
                
                PredictedEventType.HIGH_MILEAGE -> 
                    actions.add("Avaliar necessidade de substituição de peças")
            }
        }

        return actions
    }

    companion object {
        fun getInstance(context: Context): PredictiveAnalyticsService {
            return PredictiveAnalyticsService(context.applicationContext)
        }
    }
}
