package com.example.platerecognitionapp.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(
    tableName = "plate_locations",
    foreignKeys = [
        ForeignKey(
            entity = Plate::class,
            parentColumns = ["id"],
            childColumns = ["plateId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("plateId")]
)
data class PlateLocation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val plateId: Int,
    val latitude: Double,
    val longitude: Double,
    val capturedAt: LocalDateTime = LocalDateTime.now(),
    val accuracy: Float = 0f
)
