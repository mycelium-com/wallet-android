package com.mycelium.wapi.wallet.tron.coins

import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.tron.TronAddress

abstract class TronCoin(id: String, name: String, symbol: String)
    : CryptoCurrency(id, name, symbol, 6, 8, false) {

    override fun parseAddress(addressString: String?): Address? = parseAddress(this, addressString)

    companion object {
        @JvmStatic
        val BLOCK_TIME_IN_SECONDS = 3

        fun parseAddress(cryptoCurrency: CryptoCurrency, addressString: String?): Address? {
            val cleaned = addressString?.trim() ?: return null
            return if (TronAddress.isValid(cleaned)) TronAddress(cryptoCurrency, cleaned) else null
        }
    }
}

object TronMain : TronCoin("tron.main", "Tron", "TRX")
object TronTest : TronCoin("tron.test", "Tron test", "tTRX")
