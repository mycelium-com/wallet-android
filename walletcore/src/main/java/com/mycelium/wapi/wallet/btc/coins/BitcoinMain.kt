package com.mycelium.wapi.wallet.btc.coins

import com.mrd.bitlib.model.BitcoinAddress
import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.btc.BtcAddress
import com.mycelium.wapi.wallet.coins.CryptoCurrency

object BitcoinMain : CryptoCurrency("bitcoin.main", "Bitcoin", "BTC", 8, 8, true) {
    override fun parseAddress(addressString: String?): Address? {
        val address = BitcoinAddress.fromString(addressString) ?: return null
        try {
            if (!address.network.isProdnet) {
                return null
            }
        } catch (e: IllegalStateException) {
            return null
        }
        return BtcAddress(this, address)
    }

    @JvmStatic
    fun get() = this
}