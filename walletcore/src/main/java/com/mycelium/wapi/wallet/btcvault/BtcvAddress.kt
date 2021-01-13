package com.mycelium.wapi.wallet.btcvault

import com.mrd.bitlib.model.AddressType
import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import java.util.*

class BtcvAddress @JvmOverloads constructor(override val coinType: CryptoCurrency, val address: String, val type: AddressType = AddressType.P2PKH) : Address {
    override fun toString(): String {
        return address
    }

    override fun getBytes(): ByteArray {
        return address.toByteArray()
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val that = o as BtcvAddress
        return address == that.address
    }

    override fun hashCode(): Int {
        return Objects.hash(address)
    }

    override fun getSubType(): String = "default"
}