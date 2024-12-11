package com.mycelium.wapi.wallet

import com.mycelium.generated.wallet.database.FeeEstimation
import com.mycelium.wapi.wallet.coins.Value

// Holds fee estimations per unit
class FeeEstimationsGeneric(val low: Value,
                            val economy: Value,
                            val normal: Value,
                            val high: Value,
                            val lastCheck: Long,
                            val scale: Int = 1) {

    fun feeEstimation() = FeeEstimation(low.type, low, economy, normal, high, lastCheck, scale)
    override fun toString(): String {
        return "FeeEstimationsGeneric(" +
                "low=${low.toUnitsString()}," +
                "economy=${economy.toUnitsString()}," +
                "normal=${normal.toUnitsString()}," +
                "high=${high.toUnitsString()}," +
                "lastCheck=$lastCheck)"
    }
}
