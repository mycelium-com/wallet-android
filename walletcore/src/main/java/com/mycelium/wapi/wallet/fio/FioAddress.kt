package com.mycelium.wapi.wallet.fio

import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.coins.CryptoCurrency

class FioAddress(override val coinType: CryptoCurrency, val fioAddressData: FioAddressData) : Address {

    override fun getBytes(): ByteArray {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun toString(): String {
        return fioAddressData.toString()
    }

    fun getAddress(): FioAddressData {
        return fioAddressData
    }

    fun getNetwork() {

    }

    override fun getSubType(): String {
        return fioAddressData.getType().name
    }

    companion object {
        @JvmStatic
        fun fromString(addressString: String): FioAddress {
            TODO("Not yet implemented")
        }
    }
}