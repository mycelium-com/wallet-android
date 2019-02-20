package com.mycelium.wallet.activity.receive

import android.app.Application
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.coins.Value

class ReceiveGenericCoinsViewModel(application: Application) : ReceiveCoinsViewModel(application) {
    private lateinit var accountLabel: String

    override fun init(account: WalletAccount<*,*>, hasPrivateKey: Boolean, showIncomingUtxo: Boolean) {
        super.init(account, hasPrivateKey, showIncomingUtxo)
        accountLabel = account.coinType.symbol
        model = ReceiveCoinsModel(getApplication(), account, accountLabel, hasPrivateKey, showIncomingUtxo)
    }

    override fun getFormattedValue(sum: Value) = sum.toStringWithUnit()

    override fun getCurrencyName() = account.coinType.symbol

    override fun getTitle(): String {
        return if (Value.isNullOrZero(model.amount.value)) {
            context.getString(R.string.address_title, accountLabel)
        } else {
            context.getString(R.string.payment_request)
        }
    }
}