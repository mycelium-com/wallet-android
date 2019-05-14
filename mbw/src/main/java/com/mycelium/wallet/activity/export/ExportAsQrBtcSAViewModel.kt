package com.mycelium.wallet.activity.export

import android.app.Application
import com.mrd.bitlib.crypto.BipDerivationType
import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.model.AddressType
import com.mycelium.wallet.MbwManager
import java.lang.IllegalStateException

class ExportAsQrBtcSAViewModel(context: Application) : ExportAsQrMultiKeysViewModel(context) {
    override fun updateData(privateDataSelected: Boolean) {
        super.updateData(privateDataSelected)
        onToggleClicked(2)
    }

    /**
     * Updates account data based on extra toggles
     */
    override fun onToggleClicked(toggleNum: Int) {
        val privateData = model.accountData.privateData

        model.accountDataString.value = if (model.privateDataSelected.value!!) {
            privateData.get()
        } else {
            val publicDataMap = model.accountData.publicDataMap
            publicDataMap?.get(when (toggleNum) {
                1 -> BipDerivationType.BIP44
                2 -> BipDerivationType.BIP49
                3 -> BipDerivationType.BIP84
                else -> throw  java.lang.IllegalStateException("Unexpected toggle position")
            })
        }
    }

    private fun publicData(privateData: String, toggleNum: Int): String {
        val network = MbwManager.getInstance(context).network
        val privateKey = InMemoryPrivateKey.fromBase58String(privateData, network).get()
        val publicKey = privateKey.publicKey
        val addressType = when (toggleNum) {
            1 -> AddressType.P2PKH
            2 -> AddressType.P2SH_P2WPKH
            3 -> AddressType.P2WPKH
            else -> throw  IllegalStateException("Unexpected toggle position")
        }
        return publicKey.toAddress(network, addressType).toString()
    }
}