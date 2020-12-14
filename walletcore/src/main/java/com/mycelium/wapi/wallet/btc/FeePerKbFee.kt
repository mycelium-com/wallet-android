package com.mycelium.wapi.wallet.btc

import com.mycelium.wapi.wallet.Fee
import com.mycelium.wapi.wallet.coins.Value

open class FeePerKbFee(val feePerKb: Value) : Fee()