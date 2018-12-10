package com.mycelium.wapi.content.btc

import com.mycelium.wapi.content.GenericAssetUri
import com.mycelium.wapi.content.WithCallback
import com.mycelium.wapi.wallet.GenericAddress
import com.mycelium.wapi.wallet.coins.Value


class BitcoinUri(address: GenericAddress?, value: Value?, label: String?, override val callbackURL: String = "")
    : GenericAssetUri(address, value, label), WithCallback {
    companion object {
        @JvmStatic
        fun from(receivingAddress: GenericAddress, aLong: Long, transactionLabel: String, o: Any): BitcoinUri {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }
}