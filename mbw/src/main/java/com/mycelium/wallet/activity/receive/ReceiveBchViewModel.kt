package com.mycelium.wallet.activity.receive

import android.app.Application
import com.mrd.bitlib.util.CoinUtil
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.currency.CurrencyValue

class ReceiveBchViewModel(application: Application) : ReceiveCoinsViewModel(application) {
    override fun init(account: WalletAccount, hasPrivateKey: Boolean, showIncomingUtxo: Boolean) {
        super.init(account, hasPrivateKey, showIncomingUtxo)
        model = ReceiveCoinsModel(getApplication(), account, ACCOUNT_LABEL, showIncomingUtxo)
    }

    override fun getHint() = context.getString(R.string.amount_hint_denomination,
            CoinUtil.Denomination.BCH)

    override fun getFormattedValue(sum: CurrencyValue) = Utils.getFormattedValueWithUnit(sum, mbwManager.bitcoinDenomination)
            .replace("BTC", "BCH")

    override fun getCurrencyName() = context.getString(R.string.bitcoin_cash_name)

    companion object {
        private const val ACCOUNT_LABEL = "bitcoincash"
    }
}