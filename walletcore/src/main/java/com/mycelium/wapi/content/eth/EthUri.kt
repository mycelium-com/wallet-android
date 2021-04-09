package com.mycelium.wapi.content.eth

import com.mycelium.wapi.content.AssetUri
import com.mycelium.wapi.content.WithCallback
import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.coins.Value


class EthUri(address: Address?, value: Value?, label: String?, override val callbackURL: String? = null,
             val asset: String? = null, scheme: String = "ethereum")
    : AssetUri(address, value, label, scheme), WithCallback