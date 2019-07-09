package com.mycelium.wapi.wallet.coinapult

import com.mycelium.wapi.wallet.GenericAddress
import java.util.*


class CoinapultAccountContext(val id: UUID, var address: GenericAddress
                              , private var isArchived: Boolean, val currency: Currency) {

    /**
     * Is this account archived?
     */
    fun isArchived(): Boolean {
        return isArchived
    }

    /**
     * Mark this account as archived
     */
    fun setArchived(isArchived: Boolean) {
        if (this.isArchived != isArchived) {
            this.isArchived = isArchived
        }
    }

}