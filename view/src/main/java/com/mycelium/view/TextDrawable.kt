package com.mycelium.view

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.util.TypedValue


class TextDrawable(val res: Resources, val text: String) : Drawable() {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = DEFAULT_COLOR
        textAlign = Paint.Align.CENTER
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                DEFAULT_TEXTSIZE, res.displayMetrics)
    }

    override fun getIntrinsicWidth(): Int = (paint.measureText(text, 0, text.length) + .5).toInt()

    override fun getIntrinsicHeight(): Int = paint.getFontMetricsInt(paint.fontMetricsInt)

    override fun draw(canvas: Canvas) {
        val count = canvas.save()
        canvas.translate(bounds.left.toFloat(), bounds.top.toFloat())
        canvas.drawText(text, 0, text.length,
                bounds.centerX().toFloat(), bounds.centerY().toFloat() - ((paint.descent() + paint.ascent()) / 2), paint)
        canvas.restoreToCount(count);
    }

    override fun getOpacity(): Int = paint.alpha

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(filter: ColorFilter?) {
        paint.colorFilter = filter
        invalidateSelf()
    }

    /**
     * size in sp
     */
    fun setFontSize(size: Float) {
        paint.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                size, res.displayMetrics)
        invalidateSelf()
    }

    fun setFontColor(color: Int) {
        paint.color = color
        invalidateSelf()
    }

    companion object {
        const val DEFAULT_COLOR = Color.WHITE
        const val DEFAULT_TEXTSIZE = 15f
    }

}