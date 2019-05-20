package com.mycelium.wapi.wallet

import com.mycelium.wapi.wallet.coins.Value

open class GenericInput(genericAddress: GenericAddress, value: Value, isCoinbase: Boolean) : GenericOutput(genericAddress, value, isCoinbase)