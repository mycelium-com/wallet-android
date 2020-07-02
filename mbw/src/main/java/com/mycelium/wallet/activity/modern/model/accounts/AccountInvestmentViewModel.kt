package com.mycelium.wallet.activity.modern.model.accounts

import com.mycelium.wapi.wallet.GenericAddress
import com.mycelium.wapi.wallet.WalletAccount


class AccountInvestmentViewModel(val account: WalletAccount<out GenericAddress>, val balance:String) : AccountListItem {
    val accountId = account.id!!
    var label = "Investment Account"

    override fun getType(): AccountListItem.Type = AccountListItem.Type.INVESTMENT_TYPE
}