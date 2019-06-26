package com.mycelium.wallet.exchange

import com.mycelium.wapi.wallet.coins.Value


class ValueSum {
    val values = mutableListOf<Value>()

    fun add(value: Value) {
        values.add(value)
    }
}