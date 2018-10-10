package com.mycelium.wallet.activity.export

import android.app.Application
import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.model.AddressType
import com.mycelium.wallet.MbwManager

class ExportAsQrBtcSAViewModel(context: Application) : ExportAsQrMultiKeysViewModel(context) {
    override fun updateData(privateDataSelected: Boolean) {
        super.updateData(privateDataSelected)
        onToggleClicked(2)
    }

    /**
     * Updates account data based on extra toggles
     */
    override fun onToggleClicked(toggleNum: Int) {
        val privateData = model.accountData.privateData.get()
        val network = MbwManager.getInstance(context).network
        val privateKey = InMemoryPrivateKey.fromBase58String(privateData, network).get()
        val publicKey = privateKey.publicKey

        val data = when (toggleNum) {
            1 -> publicKey.toAddress(network, AddressType.P2PKH).toString()

            2 -> publicKey.toAddress(network, AddressType.P2SH_P2WPKH).toString()

            3 -> publicKey.toAddress(network, AddressType.P2WPKH).toString()

            else -> throw  java.lang.IllegalStateException("Unexpected toggle position")
        }
        if (model.privateDataSelected.value!!) {
            model.accountDataString.value = privateData
        } else {
            model.accountDataString.value = data
        }
    }
}