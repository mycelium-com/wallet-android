package com.mycelium.wapi.wallet.fio.coins

import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.families.EOSBasedCryptoCurrency

class FIOTest private constructor() : EOSBasedCryptoCurrency() {
    override fun parseAddress(addressString: String): Address {
        TODO()
    }

    companion object {
        private val instance = FIOTest()

        @Synchronized
        fun get(): CryptoCurrency {
            return instance
        }
    }

    init {
        id = "fio.test"
        name = "FIO Test"
        symbol = "tFIO"
        unitExponent = 8
    }
}