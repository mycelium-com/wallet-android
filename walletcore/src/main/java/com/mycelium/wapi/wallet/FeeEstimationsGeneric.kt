package com.mycelium.wapi.wallet

import com.mycelium.generated.wallet.database.FeeEstimation
import com.mycelium.wapi.wallet.coins.Value
import java.io.Serializable

// Holds fee estimations per unit
class FeeEstimationsGeneric(low: Value,
                            economy: Value,
                            normal: Value,
                            high: Value,
                            lastCheck: Long):
        FeeEstimation by FeeEstimation.Impl(low.type, low, economy, normal, high, lastCheck), Serializable
