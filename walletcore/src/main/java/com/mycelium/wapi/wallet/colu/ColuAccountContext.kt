package com.mycelium.wapi.wallet.colu

import com.mycelium.wapi.wallet.GenericAddress
import com.mycelium.wapi.wallet.colu.coins.ColuMain
import java.util.*


class ColuAccountContext(val id: UUID, val coinType: ColuMain
                         , val address: GenericAddress
                         , private var isArchived: Boolean, var blockHeight: Int) {

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