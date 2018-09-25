package com.mycelium.wapi.wallet.colu.coins


object RMCCoin : ColuMain() {
    init {
        id = "La4aGUPuNKZyC393pS2Nb4RJdk2WvmoaAdrRLZ"
    }

    override fun getSymbol(): String {
        return "RMC"
    }

    override fun getUnitExponent(): Int {
        return 4
    }
}

object RMCCoinTest : ColuMain() {
    init {
        id = "La8yFVyKmHGf4KWjcPqATZeTrSxXyzB3JRPxDc"
    }

    override fun getSymbol(): String {
        return "RMC"
    }

    override fun getUnitExponent(): Int {
        return 4
    }
}