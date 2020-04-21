package com.mycelium.wapi.wallet

import com.mycelium.wapi.wallet.coins.Value

open class InputViewModel(genericAddress: Address, value: Value, isCoinbase: Boolean) :
        OutputViewModel(genericAddress, value, isCoinbase)