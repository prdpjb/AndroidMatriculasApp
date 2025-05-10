package com.example.platerecognitionapp.ml

import android.content.Context
import android.graphics.Bitmap
import com.example.platerecognitionapp.data.PlateDatabase
import com.example.platerecognitionapp.utils.ErrorLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.TensorProcessor
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.io.File
import java.nio.MappedByteBuffer

class PlateRecognitionTrainer(private val context: Context) {
    private val plateDatabase = PlateDatabase.getDatabase(context)
    private val errorLogger = ErrorLogger.getInstance(context)

    private val imageProcessor = ImageProcessor.Builder()
        .add(ResizeOp(224, 224, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
        .add(NormalizeOp(0f, 255f))
        .build()

    private val tensorProcessor = TensorProcessor.Builder().build()

    private fun loadModel(): MappedByteBuffer {
        return FileUtil.loadMappedFile(context, "plate_recognition_model.tflite")
    }

    suspend fun trainModel(trainingImages: List<Bitmap>): Boolean = withContext(Dispatchers.Default) {
        try {
            val interpreter = Interpreter(loadModel())
            
            // Preparar dados de treinamento
            val trainingData = prepareTrainingData(trainingImages)
            
            // Treinar modelo
            val inputTensor = TensorImage.fromBitmap(trainingImages.first())
            val outputTensor = interpreter.getOutputTensor(0)
            
            // Simular processo de treinamento
            for (image in trainingData) {
                val processedImage = imageProcessor.process(inputTensor)
                interpreter.run(processedImage.buffer, outputTensor.buffer)
            }
            
            // Salvar modelo treinado
            saveTrainedModel(interpreter)
            
            true
        } catch (e: Exception) {
            errorLogger.logError("Erro no treinamento do modelo", e)
            false
        }
    }

    private suspend fun prepareTrainingData(images: List<Bitmap>): List<TensorImage> = withContext(Dispatchers.Default) {
        // Buscar matrículas existentes para aprendizado
        val existingPlates = plateDatabase.plateDao().getAllPlates().first()
        
        images.map { bitmap ->
            val tensorImage = TensorImage.fromBitmap(bitmap)
            imageProcessor.process(tensorImage)
        }
    }

    private fun saveTrainedModel(interpreter: Interpreter) {
        val modelFile = File(context.filesDir, "custom_plate_model.tflite")
        interpreter.saveModel(modelFile.absolutePath)
    }

    fun loadCustomModel(): Interpreter? {
        return try {
            val modelFile = File(context.filesDir, "custom_plate_model.tflite")
            if (modelFile.exists()) {
                Interpreter(FileUtil.loadMappedFile(context, modelFile.absolutePath))
            } else null
        } catch (e: Exception) {
            errorLogger.logError("Erro ao carregar modelo personalizado", e)
            null
        }
    }

    companion object {
        fun getInstance(context: Context): PlateRecognitionTrainer {
            return PlateRecognitionTrainer(context.applicationContext)
        }
    }
}
