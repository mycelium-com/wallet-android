package com.mycelium.wapi.content.colu

import com.mycelium.wapi.content.GenericAssetUri
import com.mycelium.wapi.wallet.GenericAddress
import com.mycelium.wapi.wallet.coins.Value

abstract class ColuAssetUri(address: GenericAddress?, value: Value?, label: String?, open val callbackURL: String? = null)
    : GenericAssetUri(address, value, label)
