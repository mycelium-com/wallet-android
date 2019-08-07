package com.mycelium.wapi.wallet.genericdb

import com.mycelium.generated.wallet.database.FeeEstimation
import com.mycelium.generated.wallet.database.WalletDB
import com.mycelium.wapi.wallet.FeeEstimationsGeneric
import com.mycelium.wapi.wallet.coins.GenericAssetInfo

class FeeEstimationsBacking(walletDB: WalletDB) {
    private val queries = walletDB.feeEstimationsQueries

    fun getEstimationForCurrency(currency: GenericAssetInfo): FeeEstimationsGeneric? {
        val estimation = queries.selectByCurrency(currency)
                .executeAsOneOrNull()
        return if (estimation != null) {
            FeeEstimationsGeneric(estimation.low, estimation.economy, estimation.normal, estimation.high, estimation.lastCheck)
        } else {
            null
        }
    }

    fun updateFeeEstimation(estimation: FeeEstimation) {
        queries.insertFullObject(estimation)
    }

}