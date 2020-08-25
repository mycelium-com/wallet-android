package com.mycelium.wapi.wallet.fio.coins

import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.coins.families.EOSBasedCryptoCurrency
import com.mycelium.wapi.wallet.fio.FioAddress
import com.mycelium.wapi.wallet.fio.FioAddressData
import fiofoundation.io.fiosdk.isFioPublicKey

abstract class FIOToken : EOSBasedCryptoCurrency() {
    init {
        unitExponent = 9
        symbol = "FIO"
    }

    abstract val url: String

    override fun parseAddress(addressString: String): Address? {
        return if (!addressString.isFioPublicKey()) {
            null
        } else {
            FioAddress(this, FioAddressData(addressString))
        }
    }
}