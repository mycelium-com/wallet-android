package com.mycelium.wapi.content.colu.mss

import com.mycelium.wapi.content.btc.BitcoinUri
import com.mycelium.wapi.content.colu.ColuAssetUri
import com.mycelium.wapi.wallet.GenericAddress
import com.mycelium.wapi.wallet.coins.Value

class MSSUri(address: GenericAddress?, value: Value?, label: String?, override val callbackURL: String? = null)
    : ColuAssetUri(address, value, label) {
    companion object {
        @JvmStatic
        fun from(receivingAddress: GenericAddress, aLong: Long, transactionLabel: String, o: Any): BitcoinUri {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }
}