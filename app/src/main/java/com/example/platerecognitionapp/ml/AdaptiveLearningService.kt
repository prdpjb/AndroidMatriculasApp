package com.example.platerecognitionapp.ml

import android.content.Context
import android.graphics.Bitmap
import com.example.platerecognitionapp.data.Plate
import com.example.platerecognitionapp.data.PlateDatabase
import com.example.platerecognitionapp.utils.ErrorLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

class AdaptiveLearningService(private val context: Context) {
    private val plateDatabase = PlateDatabase.getDatabase(context)
    private val errorLogger = ErrorLogger.getInstance(context)
    private val auditLogService = AuditLogService.getInstance(context)
    
    private val learningStrategies: List<LearningStrategy> = listOf(
        TransferLearningStrategy(context)
    )

    private val currentModel: ModelVersion by lazy { 
        loadLatestModel() 
    }

    private val imageProcessor = ImageProcessor.Builder()
        .add(ResizeOp(224, 224, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
        .build()

    private val tensorProcessor = TensorProcessor.Builder().build()
    private val plateDatabase = PlateDatabase.getDatabase(context)
    private val errorLogger = ErrorLogger.getInstance(context)
    private val plateRecognitionTrainer = PlateRecognitionTrainer.getInstance(context)

    suspend fun updateModelWithNewData(
        plateNumber: String, 
        capturedImage: Bitmap,
        confidence: Float
    ): Boolean = withContext(Dispatchers.Default) {
        try {
            // Verificar se a matrícula já existe
            val existingPlate = plateDatabase.plateDao()
                .getPlateByNumber(plateNumber)
                .first()

            // Se a matrícula já existe, não atualizar
            if (existingPlate != null) return@withContext false

            // Avaliar confiança da detecção
            if (confidence < CONFIDENCE_THRESHOLD) {
                // Solicitar confirmação do usuário
                val newPlate = Plate(
                    plateNumber = plateNumber,
                    capturedAt = LocalDateTime.now(),
                    confidence = confidence
                )
                
                plateDatabase.plateDao().insert(newPlate)

                // Treinar modelo com nova imagem
                plateRecognitionTrainer.trainModel(listOf(capturedImage))

                true
            } else {
                false
            }
        } catch (e: Exception) {
            errorLogger.logError("Erro no aprendizado adaptativo", e)
            false
        }
    }

    suspend fun collectLearningFeedback(
        plateNumber: String,
        isCorrect: Boolean
    ): Boolean = withContext(Dispatchers.Default) {
        try {
            val plate = plateDatabase.plateDao()
                .getPlateByNumber(plateNumber)
                .first() ?: return@withContext false

            // Atualizar confiança baseado no feedback
            val updatedPlate = plate.copy(
                confidence = if (isCorrect) plate.confidence * 1.1f 
                             else plate.confidence * 0.9f
            )

            plateDatabase.plateDao().update(updatedPlate)

            // Se o feedback for negativo, remover entrada
            if (!isCorrect) {
                plateDatabase.plateDao().delete(updatedPlate)
            }

            true
        } catch (e: Exception) {
            errorLogger.logError("Erro no feedback de aprendizado", e)
            false
        }
    }

    companion object {
        private const val CONFIDENCE_THRESHOLD = 0.7f

        fun getInstance(context: Context): AdaptiveLearningService {
            return AdaptiveLearningService(context.applicationContext)
        }
    }
}
