package com.mycelium.wallet.activity.send.model

import android.app.Activity
import android.app.Application
import android.content.Intent
import com.mycelium.wallet.activity.util.EthFeeFormatter
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.btc.bip44.HDAccountExternalSignature
import com.mycelium.wapi.wallet.coins.Value
import java.util.regex.Pattern

class SendFioViewModel(application: Application) : SendCoinsViewModel(application) {
    override val uriPattern = Pattern.compile("[a-zA-Z0-9]+")!!

    override fun init(account: WalletAccount<*>, intent: Intent) {
        super.init(account, intent)
        model = SendFioModel(getApplication(), account, intent)
    }

    override fun sendTransaction(activity: Activity) {
        if (isColdStorage() || model.account is HDAccountExternalSignature) {
            // We do not ask for pin when the key is from cold storage or from a external device (trezor,...)
            model.signTransaction(activity)
            sendFioObtData()
        } else {
            mbwManager.runPinProtectedFunction(activity) {
                model.signTransaction(activity)
                sendFioObtData()
            }
        }
    }

    override fun getFeeFormatter() = EthFeeFormatter()

    fun fee(value: Value?): String = value?.toStringWithUnit() ?: SendFioModel.DEFAULT_FEE

    fun feeFiat(value: Value?): String =
            "~${mbwManager.exchangeRateManager.get(value, mbwManager.getFiatCurrency(model.account.coinType))?.toStringWithUnit()}"
}