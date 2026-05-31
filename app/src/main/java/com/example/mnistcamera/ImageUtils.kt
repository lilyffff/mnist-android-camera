package com.example.mnistcamera

import android.graphics.Bitmap

object ImageUtils {

    private const val TARGET_SIZE = 28
    private const val MNIST_MEAN = 0.1307f
    private const val MNIST_STD = 0.3081f

    fun preprocess(bitmap: Bitmap): FloatArray {
        val cropped = centerCrop(bitmap)
        val resized = Bitmap.createScaledBitmap(cropped, TARGET_SIZE, TARGET_SIZE, true)

        val pixels = IntArray(TARGET_SIZE * TARGET_SIZE)
        resized.getPixels(pixels, 0, TARGET_SIZE, 0, 0, TARGET_SIZE, TARGET_SIZE)

        val output = FloatArray(TARGET_SIZE * TARGET_SIZE)
        for (i in pixels.indices) {
            val px = pixels[i]
            val r = (px shr 16) and 0xFF
            val g = (px shr 8) and 0xFF
            val b = px and 0xFF

            val gray = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
            val inverted = 1f - gray
            output[i] = (inverted - MNIST_MEAN) / MNIST_STD
        }

        return output
    }

    private fun centerCrop(bitmap: Bitmap): Bitmap {
        val size = minOf(bitmap.width, bitmap.height)
        val x = (bitmap.width - size) / 2
        val y = (bitmap.height - size) / 2
        return Bitmap.createBitmap(bitmap, x, y, size, size)
    }
}
