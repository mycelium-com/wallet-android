package com.mycelium.wapi.wallet.eth.coins

import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.SoftDustPolicy

class EthTest private constructor(): EthCoin() {
    //TODO: create init
    init {
        id = "ethereum.test"
        name = "Ethereum Test"
        symbol = "ETH"
        uriScheme = "ethereum"
        softDustPolicy = SoftDustPolicy.AT_LEAST_BASE_FEE_IF_SOFT_DUST_TXO_PRESENT
        signedMessageHeader = toBytes("Ethereum Signed Message:\n")
    }

    companion object {
        private val instance = EthTest()
        @Synchronized
        @JvmStatic
        fun get(): CryptoCurrency {
            return instance
        }
    }
}