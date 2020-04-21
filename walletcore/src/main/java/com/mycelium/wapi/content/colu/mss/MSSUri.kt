package com.mycelium.wapi.content.colu.mss

import com.mycelium.wapi.content.WithCallback
import com.mycelium.wapi.content.colu.ColuAssetUri
import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.coins.Value

class MSSUri(address: Address?, value: Value?, label: String?, override val callbackURL: String?, scheme: String = "mss")
    : ColuAssetUri(address, value, label, scheme), WithCallback {

    constructor(address: Address?, value: Value?, label: String?) : this(address,value,label,null)

    override fun equals(other: Any?): Boolean {
        if (other !is ColuAssetUri) {
            return false
        }
        return this.address == other.address
                && this.value == other.value
                && this.label == other.label
                && this.callbackURL == other.callbackURL
                && (other.scheme == "mass" || other.scheme == "mss")
    }
}