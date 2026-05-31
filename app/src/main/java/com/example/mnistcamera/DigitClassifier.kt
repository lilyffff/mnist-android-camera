package com.example.mnistcamera

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
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

    private fun loadModelFile(context: Context, modelName: String): MappedByteBuffer {
        context.assets.openFd(modelName).use { fileDescriptor ->
            FileInputStream(fileDescriptor.fileDescriptor).use { inputStream ->
                val channel = inputStream.channel
                return channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    fileDescriptor.startOffset,
                    fileDescriptor.declaredLength
                )
            }
        }
    }
}
