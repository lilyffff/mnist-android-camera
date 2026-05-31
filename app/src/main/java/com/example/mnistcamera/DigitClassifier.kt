package com.example.mnistcamera

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

data class ClassificationResult(
    val digit: Int,
    val confidence: Float
)

class DigitClassifier(context: Context) {

    private val interpreter: Interpreter = Interpreter(loadModelFile(context, "mnist_nn_model.tflite"))

    fun classify(bitmap: Bitmap): ClassificationResult {
        val inputData = ImageUtils.preprocess(bitmap)
        val input = arrayOf(inputData)
        val output = Array(1) { FloatArray(10) }

        interpreter.run(input, output)

        var maxIndex = 0
        var maxScore = output[0][0]
        for (i in 1 until output[0].size) {
            if (output[0][i] > maxScore) {
                maxScore = output[0][i]
                maxIndex = i
            }
        }

        return ClassificationResult(maxIndex, maxScore)
    }

    fun close() {
        interpreter.close()
    }

    private fun loadModelFile(context: Context, modelName: String): ByteBuffer {
        val modelFile = File(context.codeCacheDir, modelName)
        if (!modelFile.exists()) {
            context.assets.open(modelName).use { inputStream ->
                FileOutputStream(modelFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }

        FileInputStream(modelFile).use { inputStream ->
            val channel = inputStream.channel
            return channel.map(
                FileChannel.MapMode.READ_ONLY,
                0,
                channel.size()
            )
        }
    }
}
