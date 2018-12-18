package com.mycelium.wapi.content.colu.mt

import com.mycelium.wapi.content.WithCallback
import com.mycelium.wapi.content.colu.ColuAssetUri
import com.mycelium.wapi.wallet.GenericAddress
import com.mycelium.wapi.wallet.coins.Value

class MTUri(address: GenericAddress?, value: Value?, label: String?, override val callbackURL: String?, scheme: String = "rmc")
    : ColuAssetUri(address, value, label, scheme), WithCallback {

    constructor(address: GenericAddress?, value: Value?, label: String?) : this(address,value,label,null)
}