package com.mycelium.wapi.wallet.manager

import com.mycelium.wapi.wallet.coins.COINS
import com.mycelium.wapi.wallet.coins.AssetInfo
import com.mycelium.wapi.wallet.providers.FeeProvider
import kotlinx.coroutines.*
import kotlin.collections.HashMap

class FeeEstimations {
    private val feeProviders = HashMap<AssetInfo, FeeProvider>(COINS.size / 2 + 1)

    fun addProvider(provider: FeeProvider) {
        feeProviders[provider.coinType] = provider
    }

    fun getProvider(asset: AssetInfo) = feeProviders[asset]


    /**
     * This function must be triggered when wallet refreshes accounts/currency prices, e.t.c
     */
    fun triggerRefresh() {
        feeProviders.values.forEach {
            GlobalScope.launch {
                it.updateFeeEstimationsAsync()
            }
        }
    }
}