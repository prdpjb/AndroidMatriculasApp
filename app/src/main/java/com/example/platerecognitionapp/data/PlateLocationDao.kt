package com.example.platerecognitionapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface PlateLocationDao {
    @Insert
    suspend fun insert(plateLocation: PlateLocation)

    @Query("SELECT * FROM plate_locations WHERE plateId = :plateId ORDER BY capturedAt DESC")
    fun getLocationsByPlate(plateId: Int): Flow<List<PlateLocation>>

    @Query("SELECT * FROM plate_locations WHERE capturedAt BETWEEN :startTime AND :endTime")
    fun getLocationsByTimeRange(
        startTime: LocalDateTime, 
        endTime: LocalDateTime
    ): Flow<List<PlateLocation>>

    @Query("DELETE FROM plate_locations WHERE plateId = :plateId")
    suspend fun deleteLocationsByPlate(plateId: Int)

    @Query("SELECT COUNT(*) FROM plate_locations")
    fun getTotalLocationsCount(): Flow<Int>

    @Query("SELECT AVG(accuracy) FROM plate_locations")
    fun getAverageLocationAccuracy(): Flow<Float>
}
