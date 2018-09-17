package com.mycelium.wapi.wallet.colu

import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.SoftDustPolicy
import com.mycelium.wapi.wallet.coins.families.BitcoinBasedCryptoCurrency


object ColuMain : BitcoinBasedCryptoCurrency() {

    init {
        id = "colu.main"

        addressHeader = 0
        p2shHeader = 5
        acceptableAddressCodes = intArrayOf(addressHeader, p2shHeader)
        spendableCoinbaseDepth = 100
        dumpedPrivateKeyHeader = 128

//        name = "Bitcoin"
//        symbol = "BTC"
//        uriScheme = "bitcoin"
//        bip44Index = 0
//        unitExponent = 8
        feeValue = value(12000)
        minNonDust = value(5460)
        softDustLimit = value(1000000) // 0.01 BTC
        softDustPolicy = SoftDustPolicy.AT_LEAST_BASE_FEE_IF_SOFT_DUST_TXO_PRESENT
        signedMessageHeader = CryptoCurrency.toBytes("Colu Signed Message:\n")
    }
}
