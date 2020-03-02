package com.mycelium.wapi.content.btc

import com.mycelium.wapi.content.GenericAssetUri
import com.mycelium.wapi.wallet.GenericAddress

class FallbackUri(address: GenericAddress?) : GenericAssetUri(address, null, null, null)