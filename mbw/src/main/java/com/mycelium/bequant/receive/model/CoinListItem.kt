package com.mycelium.bequant.receive.model

import com.mycelium.wapi.wallet.coins.GenericAssetInfo


data class CoinListItem(val type: Int, val coin: GenericAssetInfo? = null)