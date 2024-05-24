package com.mycelium.wapi.wallet.genericdb

import com.mycelium.generated.wallet.database.FeeEstimation
import com.mycelium.generated.wallet.database.WalletDB
import com.mycelium.wapi.wallet.FeeEstimationsGeneric
import com.mycelium.wapi.wallet.coins.AssetInfo

class FeeEstimationsBacking(walletDB: WalletDB) {
    private val queries = walletDB.feeEstimationsQueries

    fun getEstimationForCurrency(currency: AssetInfo): FeeEstimationsGeneric? =
        queries.selectByCurrency(currency).executeAsOneOrNull()?.let {
            FeeEstimationsGeneric(it.low, it.economy, it.normal, it.high, it.lastCheck, it.scale)
        }

    fun updateFeeEstimation(estimation: FeeEstimation) {
        queries.insertFullObject(estimation)
    }
}