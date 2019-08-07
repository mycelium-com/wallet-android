package com.mycelium.wapi.wallet.providers

import com.mycelium.wapi.wallet.FeeEstimationsGeneric

interface FeeProvider {
    var estimation: FeeEstimationsGeneric
    suspend fun updateFeeEstimationsAsync()
}