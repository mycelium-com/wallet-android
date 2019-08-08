package com.mycelium.wapi.wallet.providers

import com.mycelium.wapi.wallet.FeeEstimationsGeneric
import com.mycelium.wapi.wallet.coins.GenericAssetInfo

interface FeeProvider {
    val coinType: GenericAssetInfo
    var estimation: FeeEstimationsGeneric
    suspend fun updateFeeEstimationsAsync()
}