package com.mycelium.wapi.wallet.bch.coins

import com.mrd.bitlib.model.BitcoinAddress
import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.btc.BtcAddress
import com.mycelium.wapi.wallet.btc.coins.BitcoinMain
import com.mycelium.wapi.wallet.btc.coins.BitcoinTest
import com.mycelium.wapi.wallet.coins.CryptoCurrency

abstract class BchCoin(id: String?, name: String?, symbol: String?, unitExponent: Int?, friendlyDigits: Int?, isUtxosBased: Boolean)
        : CryptoCurrency(id, name, symbol, unitExponent, friendlyDigits, isUtxosBased){

    override fun parseAddress(addressString: String?): Address {
        val address = BitcoinAddress.fromString(addressString)
        return BtcAddress(if (address.network.isProdnet) BitcoinMain.get() else BitcoinTest.get(), address)
    }
}