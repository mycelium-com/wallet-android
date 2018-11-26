package com.mycelium.wapi.content.btc

import com.mycelium.wapi.content.GenericAssetUri
import com.mycelium.wapi.wallet.GenericAddress
import com.mycelium.wapi.wallet.coins.Value


open class BitcoinUri(address: GenericAddress?, value: Value?, label: String?, val callbackURL: String? = null)
    : GenericAssetUri(address, value, label) {
    companion object {
        @JvmStatic
        fun from(receivingAddress: GenericAddress, aLong: Long, transactionLabel: String, o: Any): BitcoinUri {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }
}

class PrivateKeyUri(val keyString: String, label: String?) : BitcoinUri(null, null, label)