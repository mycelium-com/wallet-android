package com.mycelium.wallet.activity.receive

import android.app.Application
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.currency.CurrencyValue

class ReceiveBtcViewModel(application: Application) : ReceiveCoinsViewModel(application) {
    override fun init(account: WalletAccount, hasPrivateKey: Boolean, showIncomingUtxo: Boolean) {
        super.init(account, hasPrivateKey, showIncomingUtxo)
        model = ReceiveCoinsModel(getApplication(), account, ACCOUNT_LABEL, hasPrivateKey, showIncomingUtxo)
    }

    override fun getHint() = context.getString(R.string.amount_hint_denomination,
                mbwManager.bitcoinDenomination.toString())

    override fun getFormattedValue(sum: CurrencyValue) = Utils.getFormattedValueWithUnit(sum, mbwManager.bitcoinDenomination)

    override fun getTitle(): String {
        return if (CurrencyValue.isNullOrZero(model.amountData.value)) {
            context.getString(R.string.address_title, context.getString(R.string.bitcoin_name))
        } else {
            context.getString(R.string.payment_request)
        }
    }

    companion object {
        private val ACCOUNT_LABEL = "bitcoin"
    }
}