package com.example.platerecognitionapp.services

import android.content.Context
import com.example.platerecognitionapp.data.Plate
import com.example.platerecognitionapp.data.PlateDatabase
import com.example.platerecognitionapp.utils.ErrorLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDateTime

data class AnomalyReport(
    val plateId: Int,
    val plateNumber: String,
    val anomalyType: AnomalyType,
    val severity: AnomalySeverity,
    val details: Map<String, String>,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

enum class AnomalyType {
    UNUSUAL_LOCATION,
    RAPID_MOVEMENT,
    FREQUENCY_DEVIATION,
    MULTIPLE_JURISDICTIONS,
    POTENTIAL_CLONING
}

enum class AnomalySeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

class AnomalyDetectionService(private val context: Context) {
    private val plateDatabase = PlateDatabase.getDatabase(context)
    private val geoLocationService = GeoLocationService.getInstance(context)
    private val routeAnalysisService = RouteAnalysisService.getInstance(context)
    private val errorLogger = ErrorLogger.getInstance(context)

    suspend fun detectAnomalies(plate: Plate): List<AnomalyReport> = withContext(Dispatchers.Default) {
        try {
            val locations = plateDatabase.plateLocationDao()
                .getLocationsByPlate(plate.id)
                .first()
                .sortedBy { it.capturedAt }

            val anomalies = mutableListOf<AnomalyReport>()

            // Detecção de Movimentação Rápida
            val rapidMovementAnomaly = detectRapidMovement(plate, locations)
            rapidMovementAnomaly?.let { anomalies.add(it) }

            // Detecção de Localização Incomum
            val unusualLocationAnomaly = detectUnusualLocation(plate, locations)
            unusualLocationAnomaly?.let { anomalies.add(it) }

            // Detecção de Desvio de Frequência
            val frequencyDeviationAnomaly = detectFrequencyDeviation(plate, locations)
            frequencyDeviationAnomaly?.let { anomalies.add(it) }

            // Detecção de Múltiplas Jurisdições
            val multipleJurisdictionsAnomaly = detectMultipleJurisdictions(plate, locations)
            multipleJurisdictionsAnomaly?.let { anomalies.add(it) }

            // Detecção de Potencial Clonagem
            val potentialCloningAnomaly = detectPotentialCloning(plate)
            potentialCloningAnomaly?.let { anomalies.add(it) }

            anomalies
        } catch (e: Exception) {
            errorLogger.logError("Erro na detecção de anomalias", e)
            emptyList()
        }
    }

    private fun detectRapidMovement(
        plate: Plate, 
        locations: List<PlateLocation>
    ): AnomalyReport? {
        if (locations.size < 2) return null

        val routeAnalysis = routeAnalysisService.analyzeRouteForPlate(plate)
        
        return if (routeAnalysis.averageSpeed > 200) { // Velocidade acima de 200 km/h
            AnomalyReport(
                plateId = plate.id,
                plateNumber = plate.plateNumber,
                anomalyType = AnomalyType.RAPID_MOVEMENT,
                severity = AnomalySeverity.HIGH,
                details = mapOf(
                    "averageSpeed" to "${routeAnalysis.averageSpeed} km/h",
                    "totalDistance" to "${routeAnalysis.totalDistance} m"
                )
            )
        } else null
    }

    private fun detectUnusualLocation(
        plate: Plate, 
        locations: List<PlateLocation>
    ): AnomalyReport? {
        val routeAnalysis = routeAnalysisService.analyzeRouteForPlate(plate)
        
        return if (routeAnalysis.routeVariation > 0.1) { // Alto desvio de rota
            AnomalyReport(
                plateId = plate.id,
                plateNumber = plate.plateNumber,
                anomalyType = AnomalyType.UNUSUAL_LOCATION,
                severity = AnomalySeverity.MEDIUM,
                details = mapOf(
                    "routeVariation" to "${routeAnalysis.routeVariation}",
                    "frequentLocations" to routeAnalysis.frequentLocations.size.toString()
                )
            )
        } else null
    }

    private fun detectFrequencyDeviation(
        plate: Plate, 
        locations: List<PlateLocation>
    ): AnomalyReport? {
        val captureFrequency = calculateCaptureFrequency(locations)
        
        return if (captureFrequency > 10) { // Mais de 10 capturas por dia
            AnomalyReport(
                plateId = plate.id,
                plateNumber = plate.plateNumber,
                anomalyType = AnomalyType.FREQUENCY_DEVIATION,
                severity = AnomalySeverity.MEDIUM,
                details = mapOf(
                    "capturesPerDay" to "$captureFrequency",
                    "totalCaptures" to locations.size.toString()
                )
            )
        } else null
    }

    private fun detectMultipleJurisdictions(
        plate: Plate, 
        locations: List<PlateLocation>
    ): AnomalyReport? {
        // Implementar lógica de detecção de múltiplas jurisdições
        // Por exemplo, verificar se há localizações em diferentes estados/países
        return null
    }

    private fun detectPotentialCloning(plate: Plate): AnomalyReport? {
        // Implementar lógica de detecção de potencial clonagem de matrícula
        // Verificar padrões de uso suspeitos ou registros duplicados
        return null
    }

    private fun calculateCaptureFrequency(locations: List<PlateLocation>): Float {
        if (locations.isEmpty()) return 0f

        val firstCapture = locations.minByOrNull { it.capturedAt }?.capturedAt
        val lastCapture = locations.maxByOrNull { it.capturedAt }?.capturedAt

        return if (firstCapture != null && lastCapture != null) {
            val daysBetween = Duration.between(firstCapture, lastCapture).toDays().toFloat()
            locations.size / (daysBetween + 1)
        } else 0f
    }

    companion object {
        fun getInstance(context: Context): AnomalyDetectionService {
            return AnomalyDetectionService(context.applicationContext)
        }
    }
}
