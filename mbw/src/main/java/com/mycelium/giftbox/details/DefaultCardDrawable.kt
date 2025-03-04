package com.mycelium.giftbox.details

import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.TypedValue
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.mycelium.view.TextDrawable
import com.mycelium.wallet.R


class DefaultCardDrawable(
    val res: Resources, val text: String, val textSp: Float = 34f
) : Drawable() {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = TextDrawable.DEFAULT_COLOR
        textAlign = Paint.Align.LEFT
        typeface = Typeface.DEFAULT_BOLD
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, textSp, res.displayMetrics)
    }
    private val textPadding =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 26f, res.displayMetrics)
    private val imagePadding =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12f, res.displayMetrics)


    override fun draw(canvas: Canvas) {
        val scaleFactor = bounds.width() / 1000f
        val textPaddingScaled = textPadding * scaleFactor
        val imagePaddingScaled = imagePadding * scaleFactor

        val customHeight = (bounds.width() * 0.65f).toInt()

        val gradient = LinearGradient(
            bounds.left.toFloat(),
            bounds.top.toFloat(),
            bounds.right.toFloat(),
            bounds.bottom.toFloat(),
            Color.parseColor("#0733A4"),
            Color.parseColor("#2DCEA7"),
            Shader.TileMode.CLAMP
        )
        val gradientPaint = Paint().apply {
            isDither = true
            shader = gradient
        }
        val count = canvas.save()
        canvas.drawRect(
            bounds.left.toFloat(),
            bounds.top.toFloat(),
            bounds.right.toFloat(),
            bounds.bottom.toFloat(),
            gradientPaint
        )

        //Draw text
        canvas.translate(textPaddingScaled, customHeight - textPaddingScaled)
        val len = paint.breakText(
            text, true,
            bounds.width() - 2 * textPaddingScaled, null
        )
        val textToDraw = text.substring(
            0, when {
                len == 0 -> 0
                len < text.length -> len - 1
                else -> len
            }
        ) + if (len < text.length) "…" else ""
        canvas.drawText(textToDraw, 0, textToDraw.length, 0f, 0f, paint)

        //Draw drawable
        val textBounds = Rect()
        paint.getTextBounds(textToDraw, 0, textToDraw.length, textBounds)
        val icDrawable = VectorDrawableCompat.create(res, R.drawable.ic_giftbox, null)!!
        canvas.translate(
            0f,
            -(textBounds.height() + imagePaddingScaled + icDrawable.intrinsicHeight * scaleFactor)
        )
        icDrawable.setBounds(
            0, 0,
            (icDrawable.intrinsicWidth * scaleFactor).toInt(),
            (icDrawable.intrinsicHeight * scaleFactor).toInt()
        )
        icDrawable.draw(canvas)
        canvas.restoreToCount(count)
    }

    override fun setAlpha(alpha: Int) {
        TODO("Not yet implemented")
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        TODO("Not yet implemented")
    }

    override fun getOpacity(): Int = paint.alpha

    override fun getIntrinsicHeight(): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        214f, res.displayMetrics
    ).toInt()
}