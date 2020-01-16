package com.mycelium.wapi.wallet.erc20.coins

object ZeroX: ERC20Token() {
    init {
        id = "0x"
        name = "0x"
        symbol = "0x"
        unitExponent = 18
    }

    override val contractAddress = "0xd676189f67CAB2D5f9b16a5c0898A0E30ed86560"
}