package com.mycelium.wapi.wallet.colu


object RMCCoin : ColuMain() {
    init {
        id = "La4aGUPuNKZyC393pS2Nb4RJdk2WvmoaAdrRLZ"
    }

    override fun getSymbol(): String {
        return "RMC"
    }
}