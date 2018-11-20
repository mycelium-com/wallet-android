package com.mycelium.wapi.content

import com.mycelium.wapi.wallet.GenericAddress
import com.mycelium.wapi.wallet.coins.Value
import java.io.Serializable


abstract class GenericAssetUri(val address: GenericAddress?, val value: Value?, val label: String?)
    : Serializable