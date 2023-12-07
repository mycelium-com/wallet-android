package com.mycelium.wallet

import android.content.Context
import android.view.View
import android.widget.TextView
import com.google.common.base.Strings
import com.mycelium.wallet.databinding.EnterLedgerPinDialogBinding

class LedgerPinDialog(context: Context, hidden: Boolean) : PinDialog(context, hidden, true) {
    private var pinDisp: TextView? = null

    override fun loadLayout() {
        setContentView(EnterLedgerPinDialogBinding.inflate(layoutInflater).apply {
            numpadBinding = this.keyboard.numPad
        }.root)
    }

    override fun initPinPad() {
        super.initPinPad()
        disps = listOf()
        pinDisp = findViewById<View>(R.id.pin_display) as TextView?
        numpadBinding?.pinBack?.setText(R.string.ok)
        numpadBinding?.pinBack?.setOnClickListener { acceptPin() }
    }

    override fun updatePinDisplay() {
        pinDisp!!.text = Strings.repeat(PLACEHOLDER_TYPED, enteredPin.length)
        checkPin()
    }

    override fun checkPin() {
        if (enteredPin.length >= MAX_PIN_LENGTH) {
            acceptPin()
        }
    }

    companion object {
        const val MAX_PIN_LENGTH = 32
    }
}