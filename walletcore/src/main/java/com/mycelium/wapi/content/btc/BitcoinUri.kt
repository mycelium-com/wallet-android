package com.mycelium.wapi.content.btc

import com.mycelium.wapi.content.GenericAssetUri
import com.mycelium.wapi.content.WithCallback
import com.mycelium.wapi.wallet.GenericAddress
import com.mycelium.wapi.wallet.coins.Value
import java.lang.StringBuilder


class BitcoinUri(address: GenericAddress?, value: Value?, label: String?, override val callbackURL: String? = null, scheme: String = "bitcoin")
    : GenericAssetUri(address, value, label, scheme), WithCallback {
    companion object {
        @JvmStatic
        fun from(address: GenericAddress?, value: Value?, label: String?, callbackURL: String? = null): BitcoinUri {
            return BitcoinUri(address, value, label, callbackURL)
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

    override fun toString(): String {
        var result = StringBuilder("bitcoin:")

        // detect first parameter
        var isFirst = true
        address?.run { result.append(address.toString()) }
        if (value != null) {
            if (isFirst) {
                result.append("?")
            }
            result.append("amount=").append(value!!.getValue().toDouble() / 100000000)
            isFirst = false
        }
        label?.run {
            if (isFirst) {
                result.append("?")
                isFirst = false
            } else {
                result.append("&")
            }
            result.append("label=").append(label)
        }
        callbackURL?.run {
            if (isFirst) {
                result.append("?")
                isFirst = false
            } else {
                result.append("&")
            }
            result.append("r=").append(callbackURL)
        }
        return result.toString()
    }
}