package com.mycelium.wapi.wallet

import com.mycelium.wapi.wallet.coins.Value

open class GenericInputViewModel(genericAddress: GenericAddress, value: Value, isCoinbase: Boolean) : GenericOutputViewModel(genericAddress, value, isCoinbase)