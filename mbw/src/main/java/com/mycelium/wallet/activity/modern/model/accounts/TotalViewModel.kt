package com.mycelium.wallet.activity.modern.model.accounts

import com.mycelium.wapi.wallet.currency.CurrencySum

/**
 * Model for the total item on the accounts tab.
 */
class TotalViewModel(val balance: CurrencySum) : AccountListItem {
    override fun getType() = AccountListItem.Type.TOTAL_BALANCE_TYPE
}