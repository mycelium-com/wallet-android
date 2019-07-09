package com.mycelium.wapi.wallet.btc

import com.mycelium.wapi.wallet.GenericFee
import com.mycelium.wapi.wallet.coins.Value

open class FeePerKbFee(val feePerKb: Value) : GenericFee()