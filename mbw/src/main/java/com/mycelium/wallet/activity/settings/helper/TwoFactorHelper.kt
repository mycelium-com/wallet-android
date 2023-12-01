package com.mycelium.wallet.activity.settings.helper

import com.mycelium.wallet.PinDialog
import com.mycelium.wallet.activity.util.Pin


class TwoFactorHelper(private val pinDialog: PinDialog) {
    var isFingerprintSuccess = false
    var enteredPin: Pin = Pin("")
    var listener: PinDialog.OnPinEntered? = null

    fun pinEntered(pin: Pin) {
        enteredPin = pin
        checkAndCall()
    }

    fun fingerprintSuccess() {
        isFingerprintSuccess = true
        checkAndCall()
    }

    private fun checkAndCall() {
        if (isFingerprintSuccess && enteredPin.isSet) {
            listener?.pinEntered(pinDialog, enteredPin)
        }
    }
}