package com.mycelium.wapi.wallet.fio

import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.fio.coins.FIOMain

class FioAddress(override val coinType: CryptoCurrency, val fioAddressData: FioAddressData) : Address {

    override fun getBytes(): ByteArray {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun toString(): String {
        return fioAddressData.formatPubKey
    }

    fun getAddress(): FioAddressData {
        return fioAddressData
    }

    fun getNetwork() {

    }

    override fun getSubType(): String {
        return FIOMain.name
    }

    companion object {
        @JvmStatic
        fun fromString(addressString: String): FioAddress {
            TODO("Not yet implemented")
        }
    }
}