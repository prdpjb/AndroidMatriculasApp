package com.example.platerecognitionapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "plates")
data class Plate(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val plateNumber: String,
    val capturedAt: LocalDateTime = LocalDateTime.now(),
    val confidence: Float = 0.5f,  // Valor padrão de confiança
    val learningFeedback: List<Boolean> = emptyList()  // Histórico de feedback
)
