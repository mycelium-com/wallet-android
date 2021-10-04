package com.mycelium.giftbox.details

import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.TypedValue
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.mycelium.view.TextDrawable
import com.mycelium.wallet.R


class DefaultCardDrawable(val res: Resources, val text: String) : Drawable() {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = TextDrawable.DEFAULT_COLOR
        textAlign = Paint.Align.LEFT
        typeface = Typeface.DEFAULT_BOLD
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 34f, res.displayMetrics)
    }
    private val textPadding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 26f, res.displayMetrics)
    private val imagePadding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12f, res.displayMetrics)


    override fun draw(canvas: Canvas) {
        val gradient = LinearGradient(bounds.left.toFloat(), bounds.top.toFloat(), bounds.right.toFloat(), bounds.bottom.toFloat(),
                Color.parseColor("#0733A4"), Color.parseColor("#2DCEA7"), Shader.TileMode.CLAMP)
        val gradientPaint = Paint().apply {
            isDither = true
            shader = gradient
        }
        val count = canvas.save()
        canvas.drawRect(bounds.left.toFloat(), bounds.top.toFloat(), bounds.right.toFloat(), bounds.bottom.toFloat(), gradientPaint)
        canvas.translate(textPadding, bounds.bottom.toFloat() - textPadding)
        val len = paint.breakText(text, true, bounds.right.toFloat() - 2 * textPadding, null)
        canvas.drawText(text.substring(0, if (len < text.length) len - 1 else len) + if (len < text.length) "…" else "",
                0, len,
                bounds.left.toFloat(), 0f, paint)
        VectorDrawableCompat.create(res, R.drawable.ic_giftbox, null)?.apply {
            val bounds = Rect()
            paint.getTextBounds(text, 0, text.length, bounds)
            canvas.translate(0f, -bounds.height() - intrinsicHeight - imagePadding)
            setBounds(0, 0, intrinsicWidth, intrinsicHeight)
        }?.draw(canvas)
        canvas.restoreToCount(count)
    }

    override fun setAlpha(alpha: Int) {
        TODO("Not yet implemented")
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        TODO("Not yet implemented")
    }

    override fun getOpacity(): Int = paint.alpha

    override fun getIntrinsicHeight(): Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
            214f, res.displayMetrics).toInt()
}