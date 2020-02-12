package com.mycelium.wapi.wallet.bch.coins

object BchMain: BchCoin() {

    init {
        id = "bitcoin_cash.main"
        name = "Bitcoin Cash"
        symbol = "BCH"
        unitExponent = 8
    }

    override fun getUnitExponent(): Int {
        return 0
    }
}