package com.mycelium.wapi.wallet.eth.coins

import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.SoftDustPolicy

class EthMain private constructor(): EthCoin() {
    //TODO: create init
    init {
        id = "ethereum.main"
        name = "Ethereum Main"
        symbol = "ETH"
        uriScheme = "ethereum"
        softDustPolicy = SoftDustPolicy.AT_LEAST_BASE_FEE_IF_SOFT_DUST_TXO_PRESENT
        signedMessageHeader = toBytes("Ethereum Signed Message:\n")
    }

    companion object {
        private val instance = EthMain()
        @Synchronized
        fun get(): CryptoCurrency {
            return instance
        }
    }
}