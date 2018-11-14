package com.mycelium.wallet.activity.receive

import android.app.Application
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.colu.ColuAccount
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.currency.CurrencyValue

class ReceiveCoCoViewModel(application: Application) : ReceiveCoinsViewModel(application) {
    private lateinit var accountLabel: String

    override fun init(account: WalletAccount, hasPrivateKey: Boolean, showIncomingUtxo: Boolean) {
        super.init(account, hasPrivateKey, showIncomingUtxo)
        accountLabel = (account as ColuAccount).label
        model = ReceiveCoinsModel(getApplication(), account, accountLabel, showIncomingUtxo)
    }

    override fun getHint() = context.getString(R.string.amount_hint_denomination, account.accountDefaultCurrency)

    override fun getFormattedValue(sum: CurrencyValue) = Utils.getColuFormattedValueWithUnit(sum)

    override fun getCurrencyName() = (account as ColuAccount).coluAsset.name!!
}