package com.example.mnistcamera

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class BoundingBoxOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val strokePaint = Paint().apply {
        color = 0xFF00E676.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }

    private var mappedBox: RectF? = null

    fun setBoxFromBitmap(box: RectF?, bitmapWidth: Int, bitmapHeight: Int) {
        if (box == null || bitmapWidth <= 0 || bitmapHeight <= 0 || width <= 0 || height <= 0) {
            mappedBox = null
            invalidate()
            return
        }

        val sx = width.toFloat() / bitmapWidth.toFloat()
        val sy = height.toFloat() / bitmapHeight.toFloat()
        mappedBox = RectF(
            box.left * sx,
            box.top * sy,
            box.right * sx,
            box.bottom * sy
        )
        invalidate()
    }

    fun clearBox() {
        mappedBox = null
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val rect = mappedBox ?: return
        canvas.drawRect(rect, strokePaint)
    }
}
