package com.mycelium.wapi.content.colu

import com.mycelium.wapi.content.AssetUri
import com.mycelium.wapi.content.WithCallback
import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.coins.Value

abstract class ColuAssetUri(address: Address?, value: Value?, label: String?, scheme: String?,
                            override val callbackURL: String? = null)
    : AssetUri(address, value, label, scheme), WithCallback {

    override fun equals(other: Any?): Boolean {
        if (other !is ColuAssetUri) {
            return false
        }
        return this.address == other.address
                && this.value == other.value
                && this.label == other.label
                && this.callbackURL == other.callbackURL
                && this.scheme == other.scheme
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (callbackURL?.hashCode() ?: 0)
        return result
    }

    override fun toString() = "ColuAssetUri(address=$address, value=$value, label=$label, scheme=$scheme, callbackURL=$callbackURL)"
}



