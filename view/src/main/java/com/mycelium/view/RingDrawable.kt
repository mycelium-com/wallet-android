package com.mycelium.view

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable


class RingDrawable(val angle: Float,
                   private val lineColor: Int = TextDrawable.DEFAULT_COLOR) : Drawable() {

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = lineColor
        textAlign = Paint.Align.CENTER
        style = Paint.Style.STROKE
        strokeWidth = STROKE_WIDTH
    }

    override fun draw(canvas: Canvas) {
        val count = canvas.save()
        canvas.drawArc(RectF(bounds.left.toFloat() + STROKE_WIDTH / 2, bounds.top.toFloat() + STROKE_WIDTH / 2,
                bounds.right.toFloat() - STROKE_WIDTH / 2, bounds.bottom.toFloat() - STROKE_WIDTH / 2),
                -90f, angle, false, paint)
        canvas.restoreToCount(count)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(filter: ColorFilter?) {
        paint.colorFilter = filter
        invalidateSelf()
    }

    override fun getOpacity(): Int = paint.alpha

    companion object {
        const val STROKE_WIDTH = 4f
    }
}