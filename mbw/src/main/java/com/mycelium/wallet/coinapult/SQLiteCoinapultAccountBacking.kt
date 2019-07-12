package com.mycelium.wallet.coinapult

import android.database.sqlite.SQLiteDatabase
import com.mycelium.wapi.wallet.FeeEstimationsGeneric
import com.mycelium.wapi.wallet.coinapult.CoinapultAccountBacking
import com.mycelium.wapi.wallet.coins.GenericAssetInfo
import java.util.*

class SQLiteCoinapultAccountBacking(id: UUID, val database: SQLiteDatabase) : CoinapultAccountBacking {
    override fun saveLastFeeEstimation(feeEstimation: FeeEstimationsGeneric?, assetType: GenericAssetInfo?) {
    }

    override fun loadLastFeeEstimation(assetType: GenericAssetInfo?): FeeEstimationsGeneric? {
        return null
    }

    override fun beginTransaction() {
    }

    override fun setTransactionSuccessful() {
    }

    override fun endTransaction() {
    }

    override fun clear() {
    }

}