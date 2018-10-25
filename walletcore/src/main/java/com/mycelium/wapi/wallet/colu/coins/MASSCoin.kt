package com.mycelium.wapi.wallet.colu.coins


object MASSCoin : ColuMain() {
    init {
        id = "La4szjzKfJyHQ75qgDEnbzp4qY8GQeDR5Z7h2W"
        name = "Mass Token"
        symbol = "MSS"
    }

    override fun getUnitExponent(): Int {
        return 0
    }
}


object MASSCoinTest : ColuMain() {
    init {
        id = "La4szjzKfJyHQ75qgDEnbzp4qY8GQeDR5Z7h2W"
        name = "Mass Token"
        symbol = "MSS"
    }


    override fun getUnitExponent(): Int {
        return 0
    }
}
