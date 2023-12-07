 package com.mycelium.wallet

import android.content.Context
import android.os.Handler
import android.os.Message
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatDialog
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import com.google.common.base.Strings
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.activity.settings.helper.TwoFactorHelper
import com.mycelium.wallet.activity.util.FingerprintHandler
import com.mycelium.wallet.activity.util.Pin
import com.mycelium.wallet.databinding.EnterPinDialogBinding
import com.mycelium.wallet.databinding.EnterPinDisplayBinding
import com.mycelium.wallet.databinding.EnterPinNumpadBinding

open class PinDialog(context: Context, val hidden: Boolean, cancelable: Boolean) :
    AppCompatDialog(context) {
    private val fingerprintHandler = FingerprintHandler().apply {
        successListener = {
            if (isTwoFactorAuth) {
//                  fingerprintHint.setEnabled(false);
                numpadBinding?.pinFinger?.isVisible = false
                twoFactorHelper.fingerprintSuccess()
            } else {
                dismiss()
                if (fingerprintCallback != null) {
                    fingerprintCallback!!.onSuccess()
                }
            }
        }
        failListener = { msg: String? ->
            Toaster(context).toast(msg!!, false)
        }
    }
    private val twoFactorHelper = TwoFactorHelper(this)
    protected var numpadBinding: EnterPinNumpadBinding? = null
    protected var pinBinding: EnterPinDisplayBinding? = null

    interface OnPinEntered {
        fun pinEntered(dialog: PinDialog, pin: Pin)
    }

    interface FingerprintCallback {
        fun onSuccess()
    }

    var buttons = listOf<Button>()
    protected var disps = listOf<TextView>()
    protected var enteredPin: String = ""
    var pinValidCallback: OnPinEntered? = null
    var fingerprintCallback: FingerprintCallback? = null
    protected var pinPadIsRandomized = MbwManager.getInstance(context).isPinPadRandomized
    protected var isTwoFactorAuth = MbwManager.getInstance(context).isTwoFactorEnabled
    protected var isFingerprintEnabled = MbwManager.getInstance(context).isFingerprintEnabled

    open fun setOnPinValid(_onPinValid: OnPinEntered?) {
        pinValidCallback = _onPinValid
        twoFactorHelper.listener = _onPinValid
    }


    protected fun initFingerprint(context: Context) {
        if (isFingerprintEnabled) {
            val result = fingerprintHandler.authenticate(context)
            if (!result) {
                Toaster(getContext()).toast(R.string.fingerprint_not_available, false)
                numpadBinding?.pinFinger?.isVisible = false
            }
        }
        numpadBinding?.pinFinger?.isVisible = enteredPin.isEmpty() && isFingerprintEnabled
    }

    override fun onStart() {
        super.onStart()

    }

    override fun onStop() {
        fingerprintHandler.successListener = null
        fingerprintHandler.failListener = null
        super.onStop()
    }

    protected open fun initPinPad() {
        disps = pinBinding?.let {
            listOf(it.pinChar1, it.pinChar2, it.pinChar3, it.pinChar4, it.pinChar5, it.pinChar6)
        }.orEmpty()
        buttons = numpadBinding?.let {
            listOf(
                it.pinButton0, it.pinButton1, it.pinButton2, it.pinButton3, it.pinButton4,
                it.pinButton5, it.pinButton6, it.pinButton7, it.pinButton8, it.pinButton9
            )
        }.orEmpty()
        (0..9).toList().let {
            if (pinPadIsRandomized) it.shuffled()
            else it
        }.forEachIndexed { i, value ->
            buttons[i].text = value.toString()
        }
        buttons.forEach {
            val num = it.text.toString().toInt()
            it.setOnClickListener { addDigit(num.toString()) }
        }
        numpadBinding?.pinBack?.setOnClickListener {
            removeLastDigit()
        }
        numpadBinding?.pinClr?.setOnClickListener {
            clearDigits()
            updatePinDisplay()
        }
    }

    protected open fun loadLayout() {
        setContentView(EnterPinDialogBinding.inflate(layoutInflater).apply {
            numpadBinding = this.keyboard.numPad
            pinBinding = this.keyboard.pinDisplay
        }.root)
        twoFactorHelper.needFingerCallback = {
            initFingerprint(context)
            numpadBinding?.pinFinger?.isVisible = true
        }
        numpadBinding?.pinFinger?.setOnClickListener {
            initFingerprint(context)
        }
    }

    protected fun addDigit(c: String) {
        enteredPin += c
        updatePinDisplay()
    }

    protected open fun updatePinDisplay() {
        disps.forEachIndexed { index, textView ->
            textView.text = getPinDigitAsString(enteredPin, index)
        }
        checkPin()
        numpadBinding?.pinFinger?.isVisible = enteredPin.isEmpty() && isFingerprintEnabled
    }

    protected fun getPinDigitAsString(pin: String, index: Int): String =
        if (pin.length > index) {
            if (hidden) PLACEHOLDER_TYPED else pin.substring(index, index + 1)
        } else {
            if (hidden) PLACEHOLDER_NOT_TYPED else PLACEHOLDER_SMALL
        }

    protected fun clearDigits() {
        enteredPin = ""
        disps.forEach {
            it.text = if (hidden) PLACEHOLDER_NOT_TYPED else PLACEHOLDER_SMALL
        }
    }

    protected fun removeLastDigit() {
        if (!Strings.isNullOrEmpty(enteredPin)) {
            enteredPin = enteredPin.substring(0, enteredPin.length - 1)
        }
        updatePinDisplay()
    }

    protected fun enableButtons(enabled: Boolean) {
        buttons.forEach {
            it.isEnabled = enabled
        }
    }

    protected open fun checkPin() {
        if (enteredPin.length >= 6) {
            acceptPin()
        }
    }

    protected fun acceptPin() {
        enableButtons(false)
        delayHandler.sendMessage(delayHandler.obtainMessage())
    }

    /**
     * Trick to make the last digit update before the dialog is disabled
     */
    val delayHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            if (isTwoFactorAuth) {
                numpadBinding?.pinClr?.isEnabled = false
                numpadBinding?.pinBack?.isEnabled = false
                twoFactorHelper.pinEntered(Pin(enteredPin))
            } else {
                // Optimized to not cause 4 bytes difference in DEX thus failing build reproducibility check
                if (pinValidCallback != null) {
                    val _pinDialog = this@PinDialog
                    pinValidCallback?.pinEntered(
                        _pinDialog,
                        Pin(enteredPin)
                    )
                }
                enableButtons(true)
                clearDigits()
            }
        }
    }

    init {
        window!!.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        setCancelable(cancelable)
        setCanceledOnTouchOutside(false)
        loadLayout()
        initPinPad()
        clearDigits()
        updatePinDisplay()
        this.setTitle(R.string.pin_enter_pin)

        fingerprintHandler.onCreate(context as FragmentActivity)

        initFingerprint(context)
    }

    companion object {
        const val PLACEHOLDER_TYPED =
            "\u25CF" // Unicode Character 'BLACK CIRCLE' (which is a white circle in our dark theme)
        const val PLACEHOLDER_NOT_TYPED =
            "\u25CB" // Unicode Character 'WHITE CIRCLE' (which is a black circle)
        const val PLACEHOLDER_SMALL = "\u2022" // Unicode Character  'BULLET'
    }
}