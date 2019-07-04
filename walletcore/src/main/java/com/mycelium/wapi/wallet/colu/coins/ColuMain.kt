package com.mycelium.wapi.wallet.colu.coins

import com.mrd.bitlib.model.Address
import com.mrd.bitlib.model.AddressType
import com.mycelium.wapi.wallet.GenericAddress
import com.mycelium.wapi.wallet.btc.BtcAddress
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.SoftDustPolicy
import com.mycelium.wapi.wallet.coins.families.BitcoinBasedCryptoCurrency


abstract class ColuMain : BitcoinBasedCryptoCurrency() {

    override fun getName(): String = name

    override fun getSymbol(): String = symbol

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is ColuMain) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    override fun parseAddress(addressString: String): GenericAddress? {
        val address = Address.fromString(addressString) ?: return null

        try {
            if (address.type === AddressType.P2WPKH)
                return null
        } catch (e: IllegalStateException) {
            return null
        }
        return BtcAddress(this, address)
    }
}
