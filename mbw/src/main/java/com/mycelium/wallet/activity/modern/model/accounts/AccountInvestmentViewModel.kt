package com.mycelium.wallet.activity.modern.model.accounts


class AccountInvestmentViewModel : AccountListItem {
    var label = "Investment Account"
    val balance = "0 BTC"

    override fun getType(): AccountListItem.Type = AccountListItem.Type.INVESTMENT_TYPE


}