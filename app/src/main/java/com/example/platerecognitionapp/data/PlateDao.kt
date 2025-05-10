package com.example.platerecognitionapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import androidx.room.OnConflictStrategy
import kotlinx.coroutines.flow.Flow

@Dao
interface PlateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plate: Plate)

    @Query("SELECT * FROM plates ORDER BY capturedAt DESC")
    fun getAllPlates(): Flow<List<Plate>>

    @Query("SELECT * FROM plates WHERE plateNumber = :plateNumber LIMIT 1")
    fun getPlateByNumber(plateNumber: String): Flow<Plate?>

    @Update
    suspend fun update(plate: Plate)

    @Delete
    suspend fun delete(plate: Plate)

    @Query("DELETE FROM plates")
    suspend fun clearAll()

    @Query("SELECT * FROM plates WHERE confidence < :threshold")
    fun getLowConfidencePlates(threshold: Float): Flow<List<Plate>>

    @Query("SELECT AVG(confidence) FROM plates")
    fun getAverageConfidence(): Flow<Float>
}
