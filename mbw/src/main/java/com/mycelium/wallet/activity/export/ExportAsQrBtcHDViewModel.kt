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
        val dataMap = if (privateData) privateDataMap else publicDataMap

        val data = dataMap?.get(when (toggleNum) {
            1 -> BipDerivationType.BIP44
            2 -> BipDerivationType.BIP49
            3 -> BipDerivationType.BIP84
            else -> throw  java.lang.IllegalStateException("Unexpected toggle position")
        })
        model.accountDataString.value = data
    }
}