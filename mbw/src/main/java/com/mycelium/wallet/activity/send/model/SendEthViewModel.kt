package com.mycelium.wallet.activity.send.model

import android.app.Activity
import android.app.Application
import android.content.Intent
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.btc.bip44.HDAccountExternalSignature

open class SendEthViewModel(context: Application) : SendCoinsViewModel(context) {

    override fun init(account: WalletAccount<*>, intent: Intent) {
        super.init(account, intent)
        model = SendEthModel(context, account, intent)
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