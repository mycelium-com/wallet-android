package com.mycelium.wapi.content.colu

import com.mycelium.wapi.content.GenericAssetUri
import com.mycelium.wapi.content.WithCallback
import com.mycelium.wapi.wallet.GenericAddress
import com.mycelium.wapi.wallet.coins.Value

abstract class ColuAssetUri(address: GenericAddress?, value: Value?, label: String?, scheme: String?,
                            override val callbackURL: String? = null)
    : GenericAssetUri(address, value, label, scheme), WithCallback {

    override fun equals(other: Any?): Boolean {
        val uri = other as ColuAssetUri
        if(this.address != uri.address || this.value != uri.value ||
                this.label != uri.label || this.callbackURL != uri.callbackURL ||
                this.scheme != uri.scheme){
            return false
        }
        return true
    }
}



