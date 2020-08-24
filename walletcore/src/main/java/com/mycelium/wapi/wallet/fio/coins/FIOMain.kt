package com.mycelium.wapi.wallet.fio.coins

import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.coins.families.EOSBasedCryptoCurrency
import com.mycelium.wapi.wallet.fio.FioAddress
import com.mycelium.wapi.wallet.fio.FioAddressData
import fiofoundation.io.fiosdk.isFioPublicKey

object FIOMain : EOSBasedCryptoCurrency() {
    override fun parseAddress(addressString: String): Address? {
        return if (!addressString.isFioPublicKey()) {
            null
        } else {
            FioAddress(this, FioAddressData(addressString))
        }
    }

    val url = "http://fioprotocol.io/v1/"
    init {
        id = "fio.main"
        name = "Fio"
        symbol = "FIO"
        unitExponent = 9
    }
}