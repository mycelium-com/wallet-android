package com.mycelium.wapi.content.btc

import com.mycelium.wapi.content.GenericAssetUri
import com.mycelium.wapi.wallet.GenericAddress
import com.mycelium.wapi.wallet.coins.Value


open class BitcoinUri(address: GenericAddress?, value: Value?, label: String?, val callbackURL: String? = null)
    : GenericAssetUri(address, value, label)

class PrivateKeyUri(val keyString: String, label: String?) : BitcoinUri(null, null, label)