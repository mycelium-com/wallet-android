package com.mycelium.wapi.wallet.fio

import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.coins.families.EOSBasedCryptoCurrency
import fiofoundation.io.fiosdk.isFioPublicKey

abstract class FIOToken : EOSBasedCryptoCurrency() {
    init {
        unitExponent = 9
        symbol = "FIO"
    }

    override fun parseAddress(addressString: String): Address? {
        return if (!addressString.isFioPublicKey()) {
            null
        } else {
            FioAddress(this, FioAddressData(addressString))
        }
    }
}