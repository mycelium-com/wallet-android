package com.mycelium.wapi.wallet.fio

import com.mrd.bitlib.bitcoinj.Base58
import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.fio.coins.FIOMain

class FioAddress(override val coinType: CryptoCurrency, private val fioAddressData: FioAddressData) : Address {

    override fun getBytes(): ByteArray = Base58.decode(fioAddressData.formatPubKey.substring(3))

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