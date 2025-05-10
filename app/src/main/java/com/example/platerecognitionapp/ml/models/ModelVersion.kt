package com.example.platerecognitionapp.ml.models

import org.tensorflow.lite.Interpreter
import java.io.File
import java.time.LocalDateTime

data class ModelVersion(
    val modelFile: File,
    val interpreter: Interpreter,
    val versionTimestamp: LocalDateTime,
    val hyperparameters: Map<String, Any> = mapOf(
        "learning_rate" to 0.001,
        "batch_size" to 32,
        "epochs" to 10
    ),
    val trainingDatasetSize: Int = 10000,
    val validationAccuracy: Float = 0.85f,
    val architectureType: String = "CNN",
    val inputShape: List<Int> = listOf(1, 224, 224, 3),
    val outputShape: List<Int> = listOf(1, 10)
) {
    fun close() {
        interpreter.close()
    }

    companion object {
        fun createDefaultModel(modelFile: File): ModelVersion {
            val interpreter = Interpreter(modelFile)
            return ModelVersion(
                modelFile = modelFile,
                interpreter = interpreter,
                versionTimestamp = LocalDateTime.now()
            )
        }
    }
}
