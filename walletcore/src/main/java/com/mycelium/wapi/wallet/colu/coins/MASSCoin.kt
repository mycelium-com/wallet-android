package com.mycelium.wapi.wallet.colu.coins


object MASSCoin : ColuMain() {
    init {
        id = "La4szjzKfJyHQ75qgDEnbzp4qY8GQeDR5Z7h2W"
    }


    override fun getSymbol() = "MSS"
    override fun getUnitExponent(): Int {
        return 0
    }
}


object MASSCoinTest : ColuMain() {
    init {
        id = "La4szjzKfJyHQ75qgDEnbzp4qY8GQeDR5Z7h2W"
    }


    override fun getSymbol() = "MSS"
    override fun getUnitExponent(): Int {
        return 0
    }
}
