package com.mycelium.wapi.wallet.colu

import com.mrd.bitlib.crypto.PublicKey
import com.mrd.bitlib.model.AddressType
import com.mycelium.wapi.wallet.btc.BtcAddress
import com.mycelium.wapi.wallet.colu.coins.ColuMain
import java.util.*


class ColuAccountContext(val id: UUID, val coinType: ColuMain
                         , val publicKey: PublicKey? = null
                         , val address: Map<AddressType, BtcAddress>? = null
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