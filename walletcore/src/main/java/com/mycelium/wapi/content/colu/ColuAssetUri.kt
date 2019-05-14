package com.mycelium.wapi.content.colu

import com.mycelium.wapi.content.GenericAssetUri
import com.mycelium.wapi.content.WithCallback
import com.mycelium.wapi.wallet.GenericAddress
import com.mycelium.wapi.wallet.coins.Value

abstract class ColuAssetUri(address: GenericAddress?, value: Value?, label: String?, scheme: String?,
                            override val callbackURL: String? = null)
    : GenericAssetUri(address, value, label, scheme), WithCallback {

    override fun equals(other: Any?): Boolean {
        other ?: return false
        val uri = other as ColuAssetUri
        if(this.address != uri.address || this.value != uri.value ||
                this.label != uri.label || this.callbackURL != uri.callbackURL ||
                this.scheme != uri.scheme){
            return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (callbackURL?.hashCode() ?: 0)
        return result
    }

    override fun toString() = "ColuAssetUri(address=$address, value=$value, label=$label, scheme=$scheme, callbackURL=$callbackURL)"
}



