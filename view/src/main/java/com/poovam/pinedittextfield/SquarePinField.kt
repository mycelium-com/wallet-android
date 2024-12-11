package com.poovam.pinedittextfield

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.util.AttributeSet
import com.mycelium.view.R


class SquarePinField : PinField {

    private var cornerRadius = 0f
        set(value) {
            field = value
            invalidate()
        }

    private val cursorPadding = Util.dpToPx(5f)

    constructor(context: Context) : super(context)

    constructor(context: Context, attr: AttributeSet) : super(context, attr) {
        initParams(attr)
    }

    constructor(context: Context, attr: AttributeSet, defStyle: Int) : super(context, attr, defStyle) {
        initParams(attr)
    }

    private fun initParams(attr: AttributeSet) {
        val a = context.theme.obtainStyledAttributes(attr, R.styleable.SquarePinField, 0, 0)
        try {
            cornerRadius = a.getDimension(R.styleable.SquarePinField_cornerRadius, cornerRadius)
        } finally {
            a.recycle()
        }
    }

    override fun onDraw(canvas: Canvas) {

        for (i in 0 until numberOfFields) {

            val x1 = (i * singleFieldWidth)
            val padding = (if (distanceInBetween != DEFAULT_DISTANCE_IN_BETWEEN) distanceInBetween else getDefaultDistanceInBetween()) / 2
            val paddedX1 = (x1 + padding)
            val paddedX2 = ((x1 + singleFieldWidth) - padding)
            val squareHeight = paddedX2 - paddedX1
            val paddedY1 = (height / 2) - (squareHeight / 2)
            val paddedY2 = (height / 2) + (squareHeight / 2)
            val textX = ((paddedX2 - paddedX1) / 2) + paddedX1
            val textY = ((paddedY2 - paddedY1) / 2 + paddedY1) + lineThickness + (textPaint.textSize / 4)
            val character: Char? = getCharAt(i)

            drawRect(canvas, paddedX1, paddedY1, paddedX2, paddedY2, fieldBgPaint)

            if (highlightAllFields() && hasFocus()) {
                drawRect(canvas, paddedX1, paddedY1, paddedX2, paddedY2, highlightPaint)
            } else {
                drawRect(canvas, paddedX1, paddedY1, paddedX2, paddedY2, fieldPaint)
            }

            if (character != null) {
                canvas.drawText(character.toString(), textX, textY, textPaint)
            }

            if (shouldDrawHint()) {
                val hintChar = hint.getOrNull(i)
                if (hintChar != null) {
                    canvas.drawText(hintChar.toString(), textX, textY, hintPaint)
                }
            }

            if (hasFocus() && i == text?.length ?: 0) {
                if (isCursorEnabled) {
                    val cursorPadding = cursorPadding + highLightThickness
                    val cursorY1 = paddedY1 + cursorPadding
                    val cursorY2 = paddedY2 - cursorPadding
                    drawCursor(canvas, textX, cursorY1, cursorY2, highlightPaint)
                }
            }
            highlightLogic(i, text?.length) {
                drawRect(canvas, paddedX1, paddedY1, paddedX2, paddedY2, highlightPaint)
            }
        }
    }

    private fun drawRect(canvas: Canvas?, paddedX1: Float, paddedY1: Float, paddedX2: Float, paddedY2: Float, paint: Paint) {
        if (cornerRadius > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            canvas?.drawRoundRect(paddedX1, paddedY1, paddedX2, paddedY2, cornerRadius, cornerRadius, paint)
        } else {
            canvas?.drawRect(paddedX1, paddedY1, paddedX2, paddedY2, paint)
        }
    }
}