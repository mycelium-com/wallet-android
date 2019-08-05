package com.mycelium.wallet.activity.settings.helper

import com.mycelium.wallet.PinDialog
import com.mycelium.wallet.activity.util.Pin


class TwoFactorHelper(val pinDialog: PinDialog, val listener: PinDialog.OnPinEntered) {
    var isFingerprintSuccess = false;
    var enteredPin: Pin = Pin("")

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
            listener.pinEntered(pinDialog, enteredPin)
        }
    }


}