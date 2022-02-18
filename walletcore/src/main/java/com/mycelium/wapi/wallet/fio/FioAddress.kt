package com.mycelium.wapi.wallet.fio

import com.mrd.bitlib.bitcoinj.Base58
import com.mrd.bitlib.model.hdpath.HdKeyPath
import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.coins.CryptoCurrency

class FioAddress(override val coinType: CryptoCurrency,
                 private val fioAddressData: FioAddressData,
                 private val subType: FioAddressSubtype = FioAddressSubtype.PUBLIC_KEY) : Address {

    override fun getBytes(): ByteArray = Base58.decode(fioAddressData.formatPubKey.substring(3))

    override fun toString(): String {
        return fioAddressData.formatPubKey
    }

    fun getAddress(): FioAddressData {
        return fioAddressData
    }

    fun getNetwork() {

    }

    override fun getSubType(): String = subType.toString()

    private var _bip32Path: HdKeyPath? = null
    override fun getBip32Path(): HdKeyPath? = _bip32Path

    override fun setBip32Path(bip32Path: HdKeyPath?) {
        _bip32Path = bip32Path
    }

    companion object {
        @JvmStatic
        fun fromString(addressString: String): FioAddress {
            TODO("Not yet implemented")
        }
    }
}

enum class FioAddressSubtype {
    PUBLIC_KEY, ADDRESS, ACTOR
}