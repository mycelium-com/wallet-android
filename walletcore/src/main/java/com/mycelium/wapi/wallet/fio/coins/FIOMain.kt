package com.mycelium.wapi.wallet.fio.coins

import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.coins.families.EOSBasedCryptoCurrency

object FIOMain : EOSBasedCryptoCurrency() {
    override fun parseAddress(addressString: String): Address {
        TODO()
    }

    init {
        id = "fio.main"
        name = "Fio"
        symbol = "FIO"
        unitExponent = 8
    }
}