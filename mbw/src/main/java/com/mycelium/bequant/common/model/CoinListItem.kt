package com.mycelium.bequant.common.model

import com.mycelium.wapi.wallet.coins.AssetInfo


data class CoinListItem(val type: Int, val coin: AssetInfo? = null)