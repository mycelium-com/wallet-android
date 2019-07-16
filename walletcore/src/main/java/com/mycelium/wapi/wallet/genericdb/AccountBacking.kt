package com.mycelium.wapi.wallet.genericdb

import com.mycelium.wapi.wallet.CommonAccountBacking
import com.mycelium.wapi.wallet.FeeEstimationsGeneric
import com.mycelium.wapi.wallet.coins.GenericAssetInfo

/**
 * Generic backing for accounts
 */
class AccountBacking: CommonAccountBacking {
    var lastFeeEstimation: FeeEstimationsGeneric? = null

    override fun clear() {
        lastFeeEstimation = null
    }

    override fun saveLastFeeEstimation(feeEstimation: FeeEstimationsGeneric?, assetType: GenericAssetInfo?) {
        lastFeeEstimation = feeEstimation
    }

    override fun loadLastFeeEstimation(assetType: GenericAssetInfo?) = lastFeeEstimation

    override fun beginTransaction() {}
    override fun setTransactionSuccessful() {}
    override fun endTransaction() {}
}