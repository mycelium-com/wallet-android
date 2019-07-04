package com.mycelium.wapi.wallet.eth.coins

import com.mycelium.wapi.wallet.coins.CryptoCurrency

class EthTest: EthCoin() {

    init {
        id = "etherium.test"
        name = "Ether test"
        symbol = "ETH"
        unitExponent = 18
    }


    override fun getUnitExponent(): Int {
        return 0
    }

    companion object {
        private val instance = EthMain()

        @Synchronized
        fun get(): CryptoCurrency {
            return instance
        }
    }
}