package com.mycelium.wallet.activity.view

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import java.math.BigDecimal

class ValueKeyboard : ConstraintLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    var inputListener: InputListener? = null
    var errorListener: ErrorListener? = null

    var inputTextView: TextView? = null
        set(v) {
//            field?.setTextColor(resources.getColor(R.color.white))
            field = v
        }

    var maxDecimals = 0
        set(v) {
            field = v
            value.setEntry(value.entryAsBigDecimal, v)
            updateDotBtn()
        }

    var spendableValue: BigDecimal? = null
        set(v) {
            field = v
            updateMaxBtn()
        }

    var maxValue: BigDecimal? = null
        set(v) {
            field = v
            checkErrors()
        }

    var minValue: BigDecimal? = null
        set(v) {
            field = v
            checkErrors()
        }

    var value = NumberEntry(maxDecimals, "", object : EntryChange {
        override fun entryChange(entry: String, wasSet: Boolean) {
            setToTextView(entry)
        }
    })

    fun setEntry(entry: String) {
        value = NumberEntry(maxDecimals, entry, object : EntryChange {
            override fun entryChange(entry: String, wasSet: Boolean) {
                setToTextView(entry)
            }
        })
    }

    private fun setToTextView(entry: String) {
        inputTextView?.let { textView ->
            textView.text = entry
            checkErrors()
        }
    }

    private fun checkErrors() = try {
        val entryValue = value.entryAsBigDecimal
        if (maxValue != null && maxValue!! < entryValue && entryValue != BigDecimal.ZERO) {
//            inputTextView?.setTextColor(resources.getColor(R.color.sender_recyclerview_background_red))
            errorListener?.maxError(maxValue!!)
        } else if (minValue != null && minValue!! > entryValue && entryValue != BigDecimal.ZERO) {
//            inputTextView?.setTextColor(resources.getColor(R.color.sender_recyclerview_background_red))
            errorListener?.minError(minValue!!)
        } else {
//            inputTextView?.setTextColor(resources.getColor(R.color.white))
            errorListener?.noError()
        }
    } catch (e: NumberFormatException) {
        errorListener?.formatError()
    }


    override fun onFinishInflate() {
        super.onFinishInflate()
        for (i in 0 until childCount) {
            val view = getChildAt(i)
            view.setOnClickListener { view ->
                when (view.id) {
                    R.id.btn_max -> {
                        value.setEntry(spendableValue, maxDecimals, true)
                    }
                    R.id.btn_backspace -> {
                        value.clicked(DEL)
                    }
                    R.id.btn_dot -> {
                        value.clicked(DOT)
                    }
                    R.id.btn_done -> {
                        done()
                    }
                    R.id.btn_copy -> {
                        val clipboardString = Utils.getClipboardString(context)
                        if (clipboardString.isNotEmpty()) {
                            try {
                                value.setEntry(BigDecimal(clipboardString), maxDecimals)
                            } catch (ignore: NumberFormatException) {
                            }
                        }
                    }
                    else -> {
                        if (view is TextView) {
                            value.clicked(view.text.toString().toInt())
                        }
                    }
                }
            }
        }
        updateDotBtn()
        updateMaxBtn()
        findViewById<View>(R.id.btn_backspace).setOnLongClickListener {
            value.clear()
            true
        }
    }

    fun setMaxText(text: String?, size: Float) {
        val textView = findViewById<TextView>(R.id.btn_max)
        textView.text = text
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
    }

    fun setPasteVisibility(visible: Boolean) {
        findViewById<View>(R.id.btn_copy).visibility = if (visible) View.VISIBLE else View.INVISIBLE
    }

    fun done() {
//        inputTextView?.setTextColor(resources.getColor(R.color.white))
        visibility = GONE
        inputListener?.done()
    }

    private fun updateDotBtn() {
        findViewById<View>(R.id.btn_dot).isEnabled = maxDecimals > 0
    }

    private fun updateMaxBtn() {
        findViewById<View>(R.id.btn_max).visibility = if (spendableValue == null) INVISIBLE else VISIBLE
    }

    interface InputListener {
        fun input(sequence: CharSequence?)
        fun max()
        fun backspace()
        fun done()
    }

    interface ErrorListener {
        fun maxError(maxValue: BigDecimal)
        fun minError(minValue: BigDecimal)
        fun formatError()
        fun noError()
    }


    open class SimpleInputListener : InputListener {
        override fun input(sequence: CharSequence?) {}
        override fun max() {}
        override fun backspace() {}
        override fun done() {}
    }

    interface EntryChange {
        fun entryChange(entry: String, wasSet: Boolean)
    }

    class NumberEntry(var _maxDecimals: Int, var entry: String, val entryChange: EntryChange) {

        init {
            if (entry.isNotEmpty()) {
                entry = try {
                    BigDecimal(entry).toPlainString()
                } catch (e: Exception) {
                    ""
                }
            }

//            if (_maxDecimals > 0) {
//                setClickListener(_llNumberEntry.findViewById(R.id.btDot) as Button, DOT)
//            } else {
//                (_llNumberEntry.findViewById(R.id.btDot) as Button).text = ""
//            }
//            setClickListener(_llNumberEntry.findViewById(R.id.btZero) as Button, 0)
//            setClickListener(_llNumberEntry.findViewById(R.id.btDel) as Button, DEL)

//            _llNumberEntry.findViewById(R.id.btDel).setOnLongClickListener {
//                entry = ""
//                _listener.onEntryChanged(entry, false)
//                true
//            }
        }

        fun clear() {
            entry = ""
            entryChange.entryChange(entry, false)
        }

        fun clicked(digit: Int) {
            if (entry == "0") {
                entry = ""
            }
            if (digit == DEL) {
                // Delete Digit
                if (entry.isNotEmpty()) {
                    entry = entry.substring(0, entry.length - 1)
                }
            } else if (digit == DOT) {
                // Do we already have a dot?
                if (hasDot()) {
                    return
                }
                if (_maxDecimals == 0) {
                    return
                }
                if (entry.isEmpty()) {
                    entry = "0."
                } else {
                    entry += '.'
                }
            } else {
                // Append Digit
                if (digit == 0 && "0" == entry) {
                    // Only one leading zero
                    return
                }
                if (hasDot()) {
                    if (decimalsAfterDot() >= _maxDecimals) {
                        // too many decimals
                        return
                    }
                } else {
                    if (decimalsBeforeDot() >= MAX_DIGITS_BEFORE_DOT) {
                        return
                    }
                }
                //                if (maxValue == null || new BigDecimal(entry + digit).compareTo(maxValue) <= 0) {
                entry = entry + digit
                //                }
            }
            entryChange.entryChange(entry, false)
        }

        private fun hasDot(): Boolean {
            return entry.indexOf('.') != -1
        }

        private fun decimalsAfterDot(): Int {
            val dotIndex = entry.indexOf('.')
            return if (dotIndex == -1) {
                0
            } else {
                entry.length - dotIndex - 1
            }
        }

        private fun decimalsBeforeDot(): Int {
            val dotIndex = entry.indexOf('.')
            return if (dotIndex == -1) {
                entry.length
            } else {
                dotIndex
            }
        }

        fun setEntry(number: BigDecimal?, maxDecimals: Int, zeroAcceptable:Boolean = false) {
            _maxDecimals = maxDecimals
            entry = if (number == null || (!zeroAcceptable && number.compareTo(BigDecimal.ZERO) == 0)) {
                ""
            } else {
                number.setScale(_maxDecimals, BigDecimal.ROUND_DOWN).stripTrailingZeros().toPlainString()
            }
            entryChange.entryChange(entry, true)
        }

        val entryAsBigDecimal: BigDecimal
            get() =
                when {
                    entry.isEmpty() -> BigDecimal.ZERO
                    "0." == entry -> BigDecimal.ZERO
                    else -> try {
                        entry.toBigDecimal()
                    } catch (e: NumberFormatException) {
                        BigDecimal.ZERO
                    }
                }
    }

    companion object {
        const val DEL = -1
        const val DOT = -2
        const val MAX_DIGITS_BEFORE_DOT = 9
    }
}