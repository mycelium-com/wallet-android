package com.mycelium.wallet.activity.export

import android.app.Application
import com.mrd.bitlib.crypto.BipDerivationType

class ExportAsQrBtcHDViewModel(context: Application) : ExportAsQrMultiKeysViewModel(context) {
    override fun updateData(privateDataSelected: Boolean) {
        super.updateData(privateDataSelected)
        onToggleClicked(1)
    }

    /**
     * Updates account data based on extra toggles
     */
    override fun onToggleClicked(toggleNum: Int) {
        val privateData = model.privateDataSelected.value!!
        val privateDataMap = model.accountData.privateDataMap
        val publicDataMap = model.accountData.publicDataMap

        val data = when (toggleNum) {
            1 -> if (privateData) {
                privateDataMap?.get(BipDerivationType.BIP44)
            } else {
                publicDataMap?.get(BipDerivationType.BIP44)
            }

            2 -> if (privateData) {
                privateDataMap?.get(BipDerivationType.BIP49)
            } else {
                publicDataMap?.get(BipDerivationType.BIP49)
            }

            3 -> if (privateData) {
                privateDataMap?.get(BipDerivationType.BIP84)
            } else {
                publicDataMap?.get(BipDerivationType.BIP84)
            }
            else -> throw  java.lang.IllegalStateException("Unexpected toggle position")
        }
        model.accountDataString.value = data
    }
}