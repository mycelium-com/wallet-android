package com.mycelium.wapi.wallet.providers

import com.mycelium.wapi.wallet.FeeEstimationsGeneric
import com.mycelium.wapi.wallet.coins.AssetInfo

interface FeeProvider {
    val coinType: AssetInfo
    var estimation: FeeEstimationsGeneric
    suspend fun updateFeeEstimationsAsync()
}