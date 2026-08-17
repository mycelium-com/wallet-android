package com.mycelium.wapi.wallet.eth.coins

object EthTest: EthCoin("ethereum.test", "Ethereum test", "tETH") {
    override val supportsEip1559 = true
}