package com.mycelium.bequant.common.model

import com.mycelium.wapi.wallet.coins.GenericAssetInfo


data class CoinListItem(val type: Int, val coin: GenericAssetInfo? = null)