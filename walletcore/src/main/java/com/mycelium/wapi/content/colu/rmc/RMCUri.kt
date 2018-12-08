package com.mycelium.wapi.content.colu.rmc

import com.mycelium.wapi.content.colu.ColuAssetUri
import com.mycelium.wapi.wallet.GenericAddress
import com.mycelium.wapi.wallet.coins.Value

class RMCUri(address: GenericAddress?, value: Value?, label: String?, override val callbackURL: String? = null)
    : ColuAssetUri(address, value, label) {
    companion object {
        @JvmStatic
        fun from(receivingAddress: GenericAddress, aLong: Long, transactionLabel: String, o: Any): ColuAssetUri {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }
}
