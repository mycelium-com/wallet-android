package com.mycelium.wallet.activity.modern.model.accounts

import com.mycelium.wapi.wallet.currency.CurrencySum

/**
 * Model for the total item on the accounts tab.
 */
class TotalViewModel(val balance: CurrencySum) : AccountListItem {
    override fun getType() = AccountListItem.Type.TOTAL_BALANCE_TYPE

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TotalViewModel

        if (balance != other.balance) return false

        return true
    }

    override fun hashCode(): Int {
        return balance.hashCode()
    }
}