package com.mycelium.wapi.wallet

import com.mycelium.generated.wallet.database.FeeEstimation
import com.mycelium.wapi.wallet.coins.Value

// Holds fee estimations per unit
class FeeEstimationsGeneric(low: Value,
                            economy: Value,
                            normal: Value,
                            high: Value,
                            lastCheck: Long,
                            scale: Int = 1):
        FeeEstimation by FeeEstimation.Impl(low.type, low, economy, normal, high, lastCheck, scale) {

    override fun toString(): String {
        return "FeeEstimationsGeneric(" +
                "low=${low.toUnitsString()}," +
                "economy=${economy.toUnitsString()}," +
                "normal=${normal.toUnitsString()}," +
                "high=${high.toUnitsString()}," +
                "lastCheck=$lastCheck)"
    }
}
