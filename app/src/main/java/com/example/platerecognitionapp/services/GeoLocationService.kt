package com.example.platerecognitionapp.services

import android.content.Context
import android.location.Location
import com.example.platerecognitionapp.data.Plate
import com.example.platerecognitionapp.utils.ErrorLogger
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

data class GeoLocation(
    val latitude: Double,
    val longitude: Double,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val accuracy: Float = 0f
)

class GeoLocationService(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }
    private val errorLogger = ErrorLogger.getInstance(context)

    suspend fun getCurrentLocation(): GeoLocation? = withContext(Dispatchers.IO) {
        try {
            val locationTask = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            )
            
            val location = Tasks.await(locationTask)
            
            location?.let {
                GeoLocation(
                    latitude = it.latitude,
                    longitude = it.longitude,
                    accuracy = it.accuracy
                )
            }
        } catch (e: SecurityException) {
            errorLogger.logError("Permissão de localização negada", e)
            null
        } catch (e: Exception) {
            errorLogger.logError("Erro ao obter localização", e)
            null
        }
    }

    suspend fun trackPlateLocation(plate: Plate): List<GeoLocation> = withContext(Dispatchers.IO) {
        try {
            // Buscar localizações históricas da matrícula no banco de dados
            val plateLocations = plateDatabase.plateLocationDao()
                .getLocationsByPlate(plate.id)
                .map { 
                    GeoLocation(
                        latitude = it.latitude,
                        longitude = it.longitude,
                        timestamp = it.capturedAt
                    )
                }
            
            plateLocations
        } catch (e: Exception) {
            errorLogger.logError("Erro ao rastrear localizações da matrícula", e)
            emptyList()
        }
    }

    suspend fun saveCurrentPlateLocation(plate: Plate) = withContext(Dispatchers.IO) {
        try {
            val currentLocation = getCurrentLocation()
            
            currentLocation?.let { location ->
                val plateLocation = PlateLocation(
                    plateId = plate.id,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    capturedAt = LocalDateTime.now(),
                    accuracy = location.accuracy
                )
                
                plateDatabase.plateLocationDao().insert(plateLocation)
            }
        } catch (e: Exception) {
            errorLogger.logError("Erro ao salvar localização da matrícula", e)
        }
    }

    suspend fun calculateDistanceBetweenLocations(
        location1: GeoLocation, 
        location2: GeoLocation
    ): Float = withContext(Dispatchers.Default) {
        val results = FloatArray(1)
        Location.distanceBetween(
            location1.latitude, location1.longitude,
            location2.latitude, location2.longitude,
            results
        )
        results[0] // Distância em metros
    }

    companion object {
        fun getInstance(context: Context): GeoLocationService {
            return GeoLocationService(context.applicationContext)
        }
    }
}
