package com.mycelium.wapi.wallet

import com.mycelium.wapi.wallet.coins.Value

class FeeEstimationsGeneric(val low: Value, var economy: Value, val normal: Value, val high: Value)