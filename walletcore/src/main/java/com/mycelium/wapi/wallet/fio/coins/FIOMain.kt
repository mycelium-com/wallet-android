package com.mycelium.wapi.wallet.fio.coins

import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.families.EOSBasedCryptoCurrency

class FIOMain private constructor() : EOSBasedCryptoCurrency() {
    override fun parseAddress(addressString: String): Address {
        TODO()
    }

    companion object {
        private val instance = FIOMain()

        @Synchronized
        fun get(): CryptoCurrency {
            return instance
        }
    }

    init {
        id = "fio.main"
        name = "Fio"
        symbol = "FIO"
        unitExponent = 8
    }
}