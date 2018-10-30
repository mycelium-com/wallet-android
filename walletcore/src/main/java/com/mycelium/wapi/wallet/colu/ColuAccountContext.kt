package com.mycelium.wapi.wallet.colu

import com.mrd.bitlib.model.AddressType
import com.mycelium.wapi.wallet.AccountBacking
import com.mycelium.wapi.wallet.GenericAddress
import com.mycelium.wapi.wallet.SingleAddressAccountBacking
import com.mycelium.wapi.wallet.WalletBacking
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

    fun getDefaultAddressType(): AddressType = AddressType.P2PKH
}