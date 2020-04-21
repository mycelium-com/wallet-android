package com.mycelium.wapi.wallet

import com.mycelium.wapi.wallet.coins.Value
import java.io.Serializable

open class OutputViewModel(val address: Address, val value: Value, val isCoinbase: Boolean) : Serializable