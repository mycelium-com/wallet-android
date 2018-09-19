package com.mycelium.wapi.wallet.colu


object MTCoin : ColuMain() {
    init {
        id = "LaA8aiRBha2BcC6PCqMuK8xzZqdA3Lb6VVv41K"
    }

    override fun getSymbol(): String {
        return "MT"
    }
}