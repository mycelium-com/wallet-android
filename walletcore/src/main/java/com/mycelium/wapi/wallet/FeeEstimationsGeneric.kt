package com.mycelium.wapi.wallet

import com.mycelium.wapi.wallet.coins.Value
import java.util.*

// Holds fee estimations per kilobyte
class FeeEstimationsGeneric(val low: Value, var economy: Value, val normal: Value, val high: Value, val lastCheck: Long)