package com.mycelium.wapi.wallet.eth.coins

object EthMain: EthCoin() {

    init {
        id = "etherium.main"
        name = "Ether"
        symbol = "ETH"
        unitExponent = 18
    }

    override fun getUnitExponent(): Int {
        return 0
    }
}