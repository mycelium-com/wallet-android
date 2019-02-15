package com.mycelium.wallet.activity.receive

import android.app.Application
import com.mycelium.view.Denomination
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.util.toString
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.coins.Value

class ReceiveBchViewModel(application: Application) : ReceiveCoinsViewModel(application) {
    override fun init(account: WalletAccount<*,*>, hasPrivateKey: Boolean, showIncomingUtxo: Boolean) {
        super.init(account, hasPrivateKey, showIncomingUtxo)
        model = ReceiveCoinsModel(getApplication(), account, ACCOUNT_LABEL, hasPrivateKey, showIncomingUtxo)
    }

    override fun getFormattedValue(sum: Value) =  sum.toString(mbwManager.denomination)

    override fun getTitle(): String {
        return if (Value.isNullOrZero(model.amount.value)) {
            context.getString(R.string.address_title, context.getString(R.string.bitcoin_cash_name))
        } else {
            context.getString(R.string.payment_request)
        }
    }

    override fun getCurrencyName() = context.getString(R.string.bitcoin_cash_name)

    companion object {
        private const val ACCOUNT_LABEL = "bitcoincash"
    }
}