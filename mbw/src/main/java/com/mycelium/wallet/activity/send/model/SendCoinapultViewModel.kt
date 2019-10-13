package com.mycelium.wallet.activity.send.model

import android.app.Activity
import android.app.Application
import com.mycelium.wapi.wallet.GenericAddress
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.btc.bip44.HDAccountExternalSignature
import com.mycelium.wapi.wallet.coins.Value

class SendCoinapultViewModel(context: Application) : SendBtcViewModel(context) {

    override fun init(account: WalletAccount<*>, amount: Value?, receivingAddress: GenericAddress?,
                      transactionLabel: String?, isColdStorage: Boolean) {
        super.init(account, amount, receivingAddress, transactionLabel, isColdStorage)
        model = SendCoinapultModel(context, account, amount, receivingAddress, transactionLabel, false)
    }

    override fun sendTransaction(activity: Activity) {
        if (isColdStorage() || model.account is HDAccountExternalSignature) {
            // We do not ask for pin when the key is from cold storage or from a external device (trezor,...)
            model.signTransaction(activity)
        } else {
            mbwManager.runPinProtectedFunction(activity) { model.signTransaction(activity) }
        }
    }
}