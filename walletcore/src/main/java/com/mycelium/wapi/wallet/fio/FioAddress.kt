package com.mycelium.wapi.wallet.fio

import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.coins.CryptoCurrency

class FioAddress(override val coinType: CryptoCurrency) : Address {
    override fun getSubType() = ""

    override fun getBytes(): ByteArray {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}