package com.mycelium.wapi.content.eth

import com.mycelium.wapi.content.GenericAssetUri
import com.mycelium.wapi.content.WithCallback
import com.mycelium.wapi.wallet.GenericAddress
import com.mycelium.wapi.wallet.coins.Value


class EthUri(address: GenericAddress?, value: Value?, label: String?, override val callbackURL: String? = null,
             scheme: String = "ethereum")
    : GenericAssetUri(address, value, label, scheme), WithCallback {
}