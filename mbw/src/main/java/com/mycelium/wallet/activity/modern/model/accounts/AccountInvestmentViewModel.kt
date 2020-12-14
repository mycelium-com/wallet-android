package com.mycelium.wallet.activity.modern.model.accounts

import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.WalletAccount


class AccountInvestmentViewModel(val account: WalletAccount<out Address>, val balance: String) : AccountListItem {
    val accountId = account.id!!
    var label = "Trading Account"

    override fun getType(): AccountListItem.Type = AccountListItem.Type.INVESTMENT_TYPE
}