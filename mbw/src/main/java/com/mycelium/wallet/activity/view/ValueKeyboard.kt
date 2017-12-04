package com.mycelium.wallet.activity.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.GridLayout
import android.widget.TextView
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.util.Util

import java.math.BigDecimal


val DEL = -1
val DOT = -2
val MAX_DIGITS_BEFORE_DOT = 9

class ValueKeyboard(context: Context?, attrs: AttributeSet?) : GridLayout(context, attrs) {

    var inputListener: InputListener? = null
    var inputTextView: TextView? = null

    var maxDecimals = 0
        set(value) {
            field = value
            this.value.setEntry(this.value.entryAsBigDecimal, value)
            updateDotBtn()
        }
    var maxValue = BigDecimal.ZERO
        set(value) {
            field = value
            updateMaxBtn()
        }

    val value = NumberEntry(maxDecimals) { entry: String, b: Boolean ->
        inputTextView?.text = entry
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        for (i in 0..childCount - 1) {
            val view = getChildAt(i)
            view.setOnClickListener { view ->
                if (view.id == R.id.btn_max) {
                    value.setEntry(maxValue, maxDecimals)
                } else if (view.id == R.id.btn_backspace) {
                    value.clicked(DEL)
                } else if (view.id == R.id.btn_dot) {
                    value.clicked(DOT)
                } else if (view.id == R.id.btn_done) {
                    inputListener?.done()
                    visibility = View.GONE
                } else if (view.id == R.id.btn_copy) {
                    value.setEntry(BigDecimal(Util.fromClipboard(context)), maxDecimals)
                } else if (view is TextView) {
                    value.clicked(view.text.toString().toInt())
                }
            }
        }
        updateDotBtn()
        updateMaxBtn()

        findViewById(R.id.btn_backspace).setOnLongClickListener {
            value.clear()
            true
        }
    }

    private fun updateDotBtn() {
        findViewById(R.id.btn_dot).isEnabled = maxDecimals > 0
    }

    private fun updateMaxBtn() {
        findViewById(R.id.btn_max).visibility = if (maxValue == BigDecimal.ZERO) View.INVISIBLE else View.VISIBLE
    }

    interface InputListener {
        fun input(char: CharSequence)
        fun max()
        fun backspace()
        fun done();
    }

    open class SimpleInputListener : InputListener {
        override fun input(char: CharSequence) {
        }

        override fun max() {
        }

        override fun backspace() {
        }

        override fun done() {
        }

    }

    class NumberEntry constructor(var _maxDecimals: Int
                                  , var entry: String = ""
                                  , var entryChange: (entry: String, wasSet: Boolean) -> Unit) {

        init {
            if (entry.isNotEmpty()) {
                try {
                    entry = BigDecimal(entry).toPlainString()
                } catch (e: Exception) {
                    entry = ""
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
            entryChange.invoke(entry, false)
        }

        fun clicked(digit: Int) {
            if (digit == DEL) {
                // Delete Digit
                if (entry.isEmpty()) {
                    return
                }
                entry = entry.substring(0, entry.length - 1)
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
                if (digit == 0 && entry == "0") {
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
                entry = entry + digit
            }
            entryChange.invoke(entry, false)
        }

        private fun hasDot(): Boolean {
            return entry.indexOf('.') != -1
        }

        private fun decimalsAfterDot(): Int {
            val dotIndex = entry.indexOf('.')
            if (dotIndex == -1) {
                return 0
            }
            return entry.length - dotIndex - 1
        }

        private fun decimalsBeforeDot(): Int {
            val dotIndex = entry.indexOf('.')
            if (dotIndex == -1) {
                return entry.length
            }
            return dotIndex
        }

        fun setEntry(number: BigDecimal?, maxDecimals: Int) {
            _maxDecimals = maxDecimals
            if (number == null || number.compareTo(BigDecimal.ZERO) == 0) {
                entry = ""
            } else {
                entry = number.setScale(_maxDecimals, BigDecimal.ROUND_HALF_DOWN).stripTrailingZeros().toPlainString()
            }
            entryChange.invoke(entry, true)
        }

        val entryAsBigDecimal: BigDecimal
            get() {
                if (entry.isEmpty()) {
                    return BigDecimal.ZERO
                }
                if (entry == "0.") {
                    return BigDecimal.ZERO
                }
                try {
                    return BigDecimal(entry)
                } catch (e: NumberFormatException) {
                    return BigDecimal.ZERO
                }

            }
    }
}