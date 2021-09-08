package com.mycelium.wapi.wallet.btcvault.coins

import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.btcvault.BtcvAddress
import com.mycelium.wapi.wallet.coins.CryptoCurrency


object BitcoinVaultTest : CryptoCurrency("bitcoinvault.test", "BitcoinVault Test", "tBTCV", 8, 2, true) {
    override fun parseAddress(addressString: String?): Address? {
        val address = BtcvAddress.fromString(this, addressString) ?: return null

        try {
            if (!address.network!!.isTestnet) {
                return null
            }
        } catch (e: IllegalStateException) {
            return null
        }
        return address
    }
}