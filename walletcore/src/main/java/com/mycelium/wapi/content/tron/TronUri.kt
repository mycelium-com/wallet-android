package com.mycelium.wapi.content.tron

import com.mycelium.wapi.content.AssetUri
import com.mycelium.wapi.content.WithCallback
import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.coins.Value

/**
 * `tron:` payment URI (TRC-21 style). For TRC20 payments [asset] carries the token contract
 * address and [address] the recipient.
 */
class TronUri(address: Address?, value: Value?, label: String?, override val callbackURL: String? = null,
              val asset: String? = null, scheme: String = "tron")
    : AssetUri(address, value, label, scheme), WithCallback
