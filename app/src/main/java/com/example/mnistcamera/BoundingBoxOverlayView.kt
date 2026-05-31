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

    private val guideFraction = 0.25f

    private val strokePaint = Paint().apply {
        color = 0xFF00E676.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }

    private var guideBox: RectF? = null

    fun getGuideRect(): RectF? {
        val current = guideBox ?: return null
        return RectF(current)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateGuideRect()
    }

    private fun updateGuideRect() {
        if (width <= 0 || height <= 0) {
            guideBox = null
            invalidate()
            return
        }

        val side = minOf(width, height) * guideFraction
        val cx = width / 2f
        val cy = height / 2f
        guideBox = RectF(
            cx - side / 2f,
            cy - side / 2f,
            cx + side / 2f,
            cy + side / 2f
        )
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val rect = guideBox ?: return
        canvas.drawRect(rect, strokePaint)
    }
}
