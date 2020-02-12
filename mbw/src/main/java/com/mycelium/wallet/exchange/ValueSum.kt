package com.mycelium.wallet.exchange

import com.mycelium.wapi.wallet.coins.Value


class ValueSum {
    val values = mutableListOf<Value>()

    fun add(value: Value) {
        values.add(value)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ValueSum

        if (values != other.values) return false

        return true
    }

    override fun hashCode(): Int {
        return values.hashCode()
    }

}