package com.mycelium.wapi.wallet.trc20.coins

import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.tron.coins.TronCoin

class TRC20Token(name: String = "", symbol: String = "", unitExponent: Int = 6, val contractAddress: String)
    : CryptoCurrency(name, name, symbol, unitExponent, 8, false) {

    override fun parseAddress(addressString: String?): Address? = TronCoin.parseAddress(this, addressString)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false
        other as TRC20Token
        return contractAddress == other.contractAddress
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + contractAddress.hashCode()
        return result
    }
}
