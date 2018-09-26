package com.mycelium.wapi.wallet.colu.coins


object MTCoin : ColuMain() {
    init {
        id = "LaA8aiRBha2BcC6PCqMuK8xzZqdA3Lb6VVv41K"
    }

    override fun getSymbol(): String {
        return "MT"
    }

    override fun getUnitExponent(): Int {
        return 7
    }
}

object MTCoinTest : ColuMain() {
    init {
        id = "La3JCiNMGmc74rcfYiBAyTUstFgmGDRDkGGCRM"
    }

    override fun getSymbol(): String {
        return "MT"
    }

    override fun getUnitExponent(): Int {
        return 7
    }
}