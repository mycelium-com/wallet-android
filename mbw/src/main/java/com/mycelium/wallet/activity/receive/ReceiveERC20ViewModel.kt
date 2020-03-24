package com.mycelium.wallet.activity.receive

import android.app.Application
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.util.toString
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.coins.Value

class ReceiveERC20ViewModel(application: Application) : ReceiveCoinsViewModel(application) {
    override fun init(account: WalletAccount<*>, hasPrivateKey: Boolean, showIncomingUtxo: Boolean) {
        super.init(account, hasPrivateKey, showIncomingUtxo)
        model = ReceiveCoinsModel(getApplication(), account, ACCOUNT_LABEL, showIncomingUtxo)
    }

    override fun getFormattedValue(sum: Value) = sum.toString(mbwManager.getDenomination(account.coinType))

    override fun getTitle(): String = if (Value.isNullOrZero(model.amount.value)) {
        context.getString(R.string.address_title, account.coinType.symbol)
    } else {
        context.getString(R.string.payment_request)
    }

    override fun getCurrencyName(): String = account.coinType.symbol

    companion object {
        private const val ACCOUNT_LABEL = "ethereum"
    }
}