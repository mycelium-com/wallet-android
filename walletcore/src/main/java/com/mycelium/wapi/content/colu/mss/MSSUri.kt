package com.mycelium.wapi.content.colu.mss

import com.mycelium.wapi.content.WithCallback
import com.mycelium.wapi.content.colu.ColuAssetUri
import com.mycelium.wapi.wallet.GenericAddress
import com.mycelium.wapi.wallet.coins.Value

class MSSUri(address: GenericAddress?, value: Value?, label: String?, override val callbackURL: String?, scheme: String = "mss")
    : ColuAssetUri(address, value, label, scheme), WithCallback {

    constructor(address: GenericAddress?, value: Value?, label: String?) : this(address,value,label,null)

    override fun equals(other: Any?): Boolean {
        val uri = other as ColuAssetUri
        if(this.address != uri.address || this.value != uri.value ||
                this.label != uri.label || this.callbackURL != uri.callbackURL ||
                !(uri.scheme == "mass" || uri.scheme == "mss")){
            return false
        }
        return true
    }
}