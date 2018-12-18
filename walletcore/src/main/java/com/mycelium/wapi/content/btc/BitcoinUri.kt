package com.mycelium.wapi.content.btc

import com.mycelium.wapi.content.GenericAssetUri
import com.mycelium.wapi.content.WithCallback
import com.mycelium.wapi.wallet.GenericAddress
import com.mycelium.wapi.wallet.coins.Value


class BitcoinUri(address: GenericAddress?, value: Value?, label: String?, override val callbackURL: String? = null, scheme: String = "bitcoin")
    : GenericAssetUri(address, value, label, scheme), WithCallback {
    companion object {
        @JvmStatic
        fun from(receivingAddress: GenericAddress, value: Value, transactionLabel: String, callbackURL: String? = null): BitcoinUri {
            return BitcoinUri(receivingAddress, value, transactionLabel, callbackURL)
        }
    }

    override fun equals(other: Any?): Boolean {
        val uri = other as BitcoinUri
        if(this.address != uri.address || this.value != uri.value ||
                this.label != uri.label || this.callbackURL != uri.callbackURL){
            return false
        }
        return true
    }
}