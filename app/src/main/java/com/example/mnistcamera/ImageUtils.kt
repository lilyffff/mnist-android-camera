package com.example.mnistcamera

import android.graphics.Bitmap

object ImageUtils {

    private const val TARGET_SIZE = 28
    private const val TARGET_DIGIT_SIZE = 20
    private const val MNIST_MEAN = 0.1307f
    private const val MNIST_STD = 0.3081f

    fun preprocess(bitmap: Bitmap): FloatArray {
        val cropped = centerCrop(bitmap)
        val width = cropped.width
        val height = cropped.height

        val invertedGray = toInvertedGray(cropped)
        val maxValue = invertedGray.maxOrNull() ?: 0f
        val threshold = maxOf(0.20f, maxValue * 0.35f)

        val digitBox = findBoundingBox(invertedGray, width, height, threshold)
        val squareBox = makePaddedSquareBox(digitBox, width, height)

        val canvas = FloatArray(TARGET_SIZE * TARGET_SIZE)
        val margin = (TARGET_SIZE - TARGET_DIGIT_SIZE) / 2

        for (y in 0 until TARGET_DIGIT_SIZE) {
            for (x in 0 until TARGET_DIGIT_SIZE) {
                val sx = squareBox.left + ((x + 0.5f) / TARGET_DIGIT_SIZE) * squareBox.width - 0.5f
                val sy = squareBox.top + ((y + 0.5f) / TARGET_DIGIT_SIZE) * squareBox.height - 0.5f
                val v = bilinearSample(invertedGray, width, height, sx, sy)

                val outX = x + margin
                val outY = y + margin
                canvas[outY * TARGET_SIZE + outX] = (v - MNIST_MEAN) / MNIST_STD
            }
        }

        return canvas
    }

    private data class Box(val left: Float, val top: Float, val width: Float, val height: Float)

    private fun toInvertedGray(bitmap: Bitmap): FloatArray {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val output = FloatArray(width * height)
        for (i in pixels.indices) {
            val px = pixels[i]
            val r = (px shr 16) and 0xFF
            val g = (px shr 8) and 0xFF
            val b = px and 0xFF
            val gray = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
            output[i] = 1f - gray
        }
        return output
    }

    private fun findBoundingBox(data: FloatArray, width: Int, height: Int, threshold: Float): Box {
        var minX = width
        var minY = height
        var maxX = -1
        var maxY = -1

        for (y in 0 until height) {
            for (x in 0 until width) {
                if (data[y * width + x] >= threshold) {
                    if (x < minX) minX = x
                    if (y < minY) minY = y
                    if (x > maxX) maxX = x
                    if (y > maxY) maxY = y
                }
            }
        }

        if (maxX < minX || maxY < minY) {
            return Box(0f, 0f, width.toFloat(), height.toFloat())
        }

        return Box(
            minX.toFloat(),
            minY.toFloat(),
            (maxX - minX + 1).toFloat(),
            (maxY - minY + 1).toFloat()
        )
    }

    private fun makePaddedSquareBox(box: Box, width: Int, height: Int): Box {
        val side = maxOf(box.width, box.height) * 1.20f
        val centerX = box.left + box.width / 2f
        val centerY = box.top + box.height / 2f

        var left = centerX - side / 2f
        var top = centerY - side / 2f
        var right = centerX + side / 2f
        var bottom = centerY + side / 2f

        if (left < 0f) {
            right -= left
            left = 0f
        }
        if (top < 0f) {
            bottom -= top
            top = 0f
        }
        if (right > width - 1f) {
            val shift = right - (width - 1f)
            left -= shift
            right = width - 1f
        }
        if (bottom > height - 1f) {
            val shift = bottom - (height - 1f)
            top -= shift
            bottom = height - 1f
        }

        left = left.coerceAtLeast(0f)
        top = top.coerceAtLeast(0f)

        val finalWidth = (right - left).coerceAtLeast(1f)
        val finalHeight = (bottom - top).coerceAtLeast(1f)
        val finalSide = minOf(finalWidth, finalHeight)

        return Box(left, top, finalSide, finalSide)
    }

    private fun bilinearSample(data: FloatArray, width: Int, height: Int, x: Float, y: Float): Float {
        val x0 = x.toInt().coerceIn(0, width - 1)
        val y0 = y.toInt().coerceIn(0, height - 1)
        val x1 = (x0 + 1).coerceIn(0, width - 1)
        val y1 = (y0 + 1).coerceIn(0, height - 1)

        val dx = (x - x0).coerceIn(0f, 1f)
        val dy = (y - y0).coerceIn(0f, 1f)

        val v00 = data[y0 * width + x0]
        val v10 = data[y0 * width + x1]
        val v01 = data[y1 * width + x0]
        val v11 = data[y1 * width + x1]

        val v0 = v00 * (1f - dx) + v10 * dx
        val v1 = v01 * (1f - dx) + v11 * dx
        return v0 * (1f - dy) + v1 * dy
    }

    private fun centerCrop(bitmap: Bitmap): Bitmap {
        val size = minOf(bitmap.width, bitmap.height)
        val x = (bitmap.width - size) / 2
        val y = (bitmap.height - size) / 2
        return Bitmap.createBitmap(bitmap, x, y, size, size)
    }
}
