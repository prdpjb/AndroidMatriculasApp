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

data class RouteAnalysis(
    val totalDistance: Float,
    val averageSpeed: Float,
    val routeVariation: Float,
    val frequentLocations: List<GeoLocation>,
    val suspiciousMovements: List<SuspiciousMovement>
)

data class SuspiciousMovement(
    val startLocation: GeoLocation,
    val endLocation: GeoLocation,
    val duration: Duration,
    val distance: Float
)

class RouteAnalysisService(private val context: Context) {
    private val plateDatabase = PlateDatabase.getDatabase(context)
    private val geoLocationService = GeoLocationService.getInstance(context)
    private val errorLogger = ErrorLogger.getInstance(context)

    suspend fun analyzeRouteForPlate(plate: Plate): RouteAnalysis = withContext(Dispatchers.Default) {
        try {
            val locations = plateDatabase.plateLocationDao()
                .getLocationsByPlate(plate.id)
                .first()
                .sortedBy { it.capturedAt }

            if (locations.size < 2) {
                return@withContext RouteAnalysis(
                    totalDistance = 0f,
                    averageSpeed = 0f,
                    routeVariation = 0f,
                    frequentLocations = emptyList(),
                    suspiciousMovements = emptyList()
                )
            }

            val geoLocations = locations.map { location ->
                GeoLocation(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    timestamp = location.capturedAt
                )
            }

            val totalDistance = calculateTotalRouteDistance(geoLocations)
            val averageSpeed = calculateAverageSpeed(geoLocations)
            val routeVariation = calculateRouteVariation(geoLocations)
            val frequentLocations = findFrequentLocations(geoLocations)
            val suspiciousMovements = detectSuspiciousMovements(geoLocations)

            RouteAnalysis(
                totalDistance = totalDistance,
                averageSpeed = averageSpeed,
                routeVariation = routeVariation,
                frequentLocations = frequentLocations,
                suspiciousMovements = suspiciousMovements
            )
        } catch (e: Exception) {
            errorLogger.logError("Erro na análise de rota", e)
            RouteAnalysis(
                totalDistance = 0f,
                averageSpeed = 0f,
                routeVariation = 0f,
                frequentLocations = emptyList(),
                suspiciousMovements = emptyList()
            )
        }
    }

    private suspend fun calculateTotalRouteDistance(locations: List<GeoLocation>): Float {
        var totalDistance = 0f
        for (i in 1 until locations.size) {
            totalDistance += geoLocationService.calculateDistanceBetweenLocations(
                locations[i-1], 
                locations[i]
            )
        }
        return totalDistance
    }

    private fun calculateAverageSpeed(locations: List<GeoLocation>): Float {
        if (locations.size < 2) return 0f

        val totalDistance = calculateTotalRouteDistance(locations)
        val totalTime = Duration.between(
            locations.first().timestamp, 
            locations.last().timestamp
        ).toSeconds()

        return if (totalTime > 0) (totalDistance / totalTime) * 3.6f else 0f
    }

    private fun calculateRouteVariation(locations: List<GeoLocation>): Float {
        // Calcular variação de rota baseado em desvios padrão de coordenadas
        val latitudes = locations.map { it.latitude }
        val longitudes = locations.map { it.longitude }

        val latMean = latitudes.average().toFloat()
        val lonMean = longitudes.average().toFloat()

        val latVariance = latitudes.map { (it - latMean) * (it - latMean) }.average().toFloat()
        val lonVariance = longitudes.map { (it - lonMean) * (it - lonMean) }.average().toFloat()

        return (latVariance + lonVariance) / 2
    }

    private fun findFrequentLocations(
        locations: List<GeoLocation>, 
        threshold: Float = 100f
    ): List<GeoLocation> {
        val frequentLocations = mutableListOf<GeoLocation>()
        
        for (location in locations) {
            val nearbyLocations = locations.filter { other ->
                geoLocationService.calculateDistanceBetweenLocations(location, other) <= threshold
            }
            
            if (nearbyLocations.size > locations.size * 0.1) {
                frequentLocations.add(location)
            }
        }

        return frequentLocations
    }

    private fun detectSuspiciousMovements(
        locations: List<GeoLocation>, 
        distanceThreshold: Float = 500f,
        timeThreshold: Duration = Duration.ofHours(1)
    ): List<SuspiciousMovement> {
        val suspiciousMovements = mutableListOf<SuspiciousMovement>()

        for (i in 1 until locations.size) {
            val distance = geoLocationService.calculateDistanceBetweenLocations(
                locations[i-1], 
                locations[i]
            )
            val duration = Duration.between(
                locations[i-1].timestamp, 
                locations[i].timestamp
            )

            if (distance > distanceThreshold && duration > timeThreshold) {
                suspiciousMovements.add(
                    SuspiciousMovement(
                        startLocation = locations[i-1],
                        endLocation = locations[i],
                        duration = duration,
                        distance = distance
                    )
                )
            }
        }

        return suspiciousMovements
    }

    companion object {
        fun getInstance(context: Context): RouteAnalysisService {
            return RouteAnalysisService(context.applicationContext)
        }
    }
}
