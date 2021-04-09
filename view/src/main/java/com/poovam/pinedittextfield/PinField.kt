package com.poovam.pinedittextfield

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.InputFilter
import android.text.method.PasswordTransformationMethod
import android.text.method.TransformationMethod
import android.util.AttributeSet
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import com.mycelium.view.R


open class PinField : AppCompatEditText {

    private val defaultWidth = Util.dpToPx(60f).toInt()

    companion object {
        const val DEFAULT_DISTANCE_IN_BETWEEN = -1f
    }

    protected var distanceInBetween: Float = DEFAULT_DISTANCE_IN_BETWEEN
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }

    var numberOfFields = 4
        set(value) {
            field = value
            limitCharsToNoOfFields()
            invalidate()
        }

    protected var singleFieldWidth = 0

    var lineThickness = Util.dpToPx(1.0f)
        set(value) {
            field = value
            fieldPaint.strokeWidth = field
            highlightPaint.strokeWidth = highLightThickness
            invalidate()
        }

    var fieldColor = ContextCompat.getColor(context, R.color.inactivePinFieldColor)
        set(value) {
            field = value
            fieldPaint.color = field
            invalidate()
        }

    var highlightPaintColor = ContextCompat.getColor(context, R.color.pinFieldLibraryAccent)
        set(value) {
            field = value
            highlightPaint.color = field
            invalidate()
        }

    var isCursorEnabled = false
        set(value) {
            field = value
            invalidate()
        }

    protected var fieldPaint = Paint()

    protected var textPaint = Paint()

    protected var hintPaint = Paint()

    protected var highlightPaint = Paint()

    protected var yPadding = Util.dpToPx(10f)

    protected var highlightSingleFieldType = HighlightType.ALL_FIELDS

    private var lastCursorChangeState: Long = -1

    private var cursorCurrentVisible = true

    private val cursorTimeout = 500L

    var isCustomBackground = false
        set(value) {
            if (!value) {
                setBackgroundResource(R.color.pinFieldLibraryTransparent)
            }
            field = value
        }

    var highLightThickness = lineThickness
        get() {
            return lineThickness + lineThickness * 0.7f
        }

    var onTextCompleteListener: OnTextCompleteListener? = null

    var fieldBgColor = ContextCompat.getColor(context, R.color.pinFieldLibraryAccent)
        set(value) {
            field = value
            fieldBgPaint.color = fieldBgColor
            invalidate()
        }

    var fieldBgPaint = Paint()

    init {
        limitCharsToNoOfFields()
        setWillNotDraw(false)
        maxLines = 1
        setSingleLine(true)

        fieldPaint.color = fieldColor
        fieldPaint.isAntiAlias = true
        fieldPaint.style = Paint.Style.STROKE
        fieldPaint.strokeWidth = lineThickness

        textPaint.color = currentTextColor
        textPaint.isAntiAlias = true
        textPaint.textSize = textSize
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.style = Paint.Style.FILL

        hintPaint.color = hintTextColors.defaultColor
        hintPaint.isAntiAlias = true
        hintPaint.textSize = textSize
        hintPaint.textAlign = Paint.Align.CENTER
        hintPaint.style = Paint.Style.FILL

        highlightPaint = Paint(fieldPaint)
        highlightPaint.color = highlightPaintColor
        highlightPaint.strokeWidth = highLightThickness

        fieldBgColor = Color.TRANSPARENT
        fieldBgPaint.style = Paint.Style.FILL
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attr: AttributeSet) : super(context, attr) {
        initParams(attr)
    }

    constructor(context: Context, attr: AttributeSet, defStyle: Int) : super(context, attr, defStyle) {
        initParams(attr)
    }

    private fun initParams(attr: AttributeSet) {
        val a = context.theme.obtainStyledAttributes(attr, R.styleable.PinField, 0, 0)

        try {
            numberOfFields = a.getInt(R.styleable.PinField_noOfFields, numberOfFields)
            lineThickness = a.getDimension(R.styleable.PinField_lineThickness, lineThickness)
            distanceInBetween = a.getDimension(R.styleable.PinField_distanceInBetween, DEFAULT_DISTANCE_IN_BETWEEN)
            fieldColor = a.getColor(R.styleable.PinField_fieldColor, fieldColor)
            highlightPaintColor = a.getColor(R.styleable.PinField_highlightColor, highlightPaintColor)
            isCustomBackground = a.getBoolean(R.styleable.PinField_isCustomBackground, false)
            isCursorEnabled = a.getBoolean(R.styleable.PinField_isCursorEnabled, false)
            highlightSingleFieldType = if (a.getBoolean(R.styleable.PinField_highlightEnabled, true)) HighlightType.ALL_FIELDS else HighlightType.NO_FIELDS
            highlightSingleFieldType = if (a.getBoolean(R.styleable.PinField_highlightSingleFieldMode, false)) HighlightType.CURRENT_FIELD else HighlightType.ALL_FIELDS
            highlightSingleFieldType = HighlightType.getEnum(a.getInt(R.styleable.PinField_highlightType, highlightSingleFieldType.code))
            fieldBgColor = a.getColor(R.styleable.PinField_fieldBgColor, fieldBgColor)
            textPaint.typeface = typeface
        } finally {
            a.recycle()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = getViewWidth(defaultWidth * numberOfFields, widthMeasureSpec)
        singleFieldWidth = width / numberOfFields
        setMeasuredDimension(width, getViewHeight(singleFieldWidth, heightMeasureSpec))
    }

    protected open fun getViewWidth(desiredWidth: Int, widthMeasureSpec: Int): Int {
        val widthMode = View.MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = View.MeasureSpec.getSize(widthMeasureSpec)

        //Measure Width
        return when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> Math.min(desiredWidth, widthSize)
            MeasureSpec.UNSPECIFIED -> desiredWidth
            else -> desiredWidth
        }
    }

    protected open fun getViewHeight(desiredHeight: Int, heightMeasureSpec: Int): Int {
        val heightMode = View.MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = View.MeasureSpec.getSize(heightMeasureSpec)

        //Measure Height
        return when (heightMode) {
            View.MeasureSpec.EXACTLY -> heightSize
            View.MeasureSpec.AT_MOST -> Math.min(desiredHeight, heightSize)
            View.MeasureSpec.UNSPECIFIED -> desiredHeight
            else -> desiredHeight
        }
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        this.setSelection(this.text!!.length)
    }

    final override fun setWillNotDraw(willNotDraw: Boolean) {
        super.setWillNotDraw(willNotDraw)
    }

    override fun onCheckIsTextEditor(): Boolean {
        return true
    }

    protected open fun getDefaultDistanceInBetween(): Float {
        return (singleFieldWidth / (numberOfFields - 1)).toFloat()
    }

    private fun limitCharsToNoOfFields() {
        val filterArray = arrayOfNulls<InputFilter>(1)
        filterArray[0] = InputFilter.LengthFilter(numberOfFields)
        filters = filterArray
    }

    override fun onTextChanged(text: CharSequence?, start: Int, lengthBefore: Int, lengthAfter: Int) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter)
        if (text != null && text.length == numberOfFields) {
            val shouldCloseKeyboard = onTextCompleteListener?.onTextComplete(text.toString())
                    ?: false
            if (shouldCloseKeyboard) {
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(windowToken, 0)
            }
        }
    }

    protected fun shouldDrawHint(): Boolean {
        return (text!!.isEmpty() || text!!.isBlank()) &&
                !isFocused && (hint != null && hint.isNotBlank() && hint.isNotEmpty())
    }

    protected fun drawCursor(canvas: Canvas?, x: Float, y1: Float, y2: Float, paint: Paint) {
        if (System.currentTimeMillis() - lastCursorChangeState > 500) {
            cursorCurrentVisible = !cursorCurrentVisible
            lastCursorChangeState = System.currentTimeMillis()
        }

        if (cursorCurrentVisible) {
            canvas?.drawLine(x, y1, x, y2, paint)
        }

        postInvalidateDelayed(cursorTimeout)
    }

    final override fun setBackgroundResource(resId: Int) {
        super.setBackgroundResource(resId)
    }

    protected fun highlightNextField(): Boolean {
        return highlightSingleFieldType == HighlightType.CURRENT_FIELD
    }

    protected fun highlightCompletedFields(): Boolean {
        return highlightSingleFieldType == HighlightType.COMPLETED_FIELDS
    }

    protected fun highlightAllFields(): Boolean {
        return highlightSingleFieldType == HighlightType.ALL_FIELDS
    }

    protected fun highlightNoFields(): Boolean {
        return highlightSingleFieldType == HighlightType.NO_FIELDS
    }

    protected fun highlightLogic(currentPosition: Int, textLength: Int?, onHighlight: () -> Unit) {
        if (hasFocus() && !highlightNoFields()) {
            when {
                highlightNextField() && currentPosition == textLength ?: 0 -> {
                    onHighlight.invoke()
                }
                highlightCompletedFields() && currentPosition < textLength ?: 0 -> {
                    onHighlight.invoke()
                }
            }
        }
    }

    //There is a issue where android transformation method is not set to password so as a work around
    //the transformation method used is hardcoded
    //the check condition used in isPassword() is taken from TextView.java constructor
    protected fun getCharAt(i: Int): Char? {
        return getPinFieldTransformation().getTransformation(text, this)?.getOrNull(i)
                ?: text?.getOrNull(i)
    }

    private fun getPinFieldTransformation(): TransformationMethod {
        if (isPassword()) {
            return PasswordTransformationMethod.getInstance();
        }

        return transformationMethod
    }

    private fun isPassword(): Boolean {
        val variation = inputType and (EditorInfo.TYPE_MASK_CLASS or EditorInfo.TYPE_MASK_VARIATION)
        val passwordInputType = (variation
                == EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_PASSWORD)
        val webPasswordInputType = (variation
                == EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_WEB_PASSWORD)
        val numberPasswordInputType = (variation
                == EditorInfo.TYPE_CLASS_NUMBER or EditorInfo.TYPE_NUMBER_VARIATION_PASSWORD)

        return passwordInputType || webPasswordInputType
                || numberPasswordInputType
    }

    interface OnTextCompleteListener {
        /**
         * @return return true if keyboard should be closed after text is entered
         */
        fun onTextComplete(enteredText: String): Boolean
    }
}

enum class HighlightType(val code: Int) {
    ALL_FIELDS(0), CURRENT_FIELD(1), COMPLETED_FIELDS(2), NO_FIELDS(3);

    companion object {
        fun getEnum(code: Int): HighlightType {
            for (type in HighlightType.values()) {
                if (type.code == code) {
                    return type
                }
            }
            return ALL_FIELDS
        }
    }
}