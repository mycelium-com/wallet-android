package com.mycelium.wapi.wallet.erc20.coins

import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.eth.coins.EthCoin

class ERC20Token(name: String = "", symbol: String = "", unitExponent: Int = 18, val contractAddress: String) : CryptoCurrency(name, name, symbol, unitExponent, 8, false) {

    override fun parseAddress(addressString: String?): Address? = EthCoin.parseAddress(this, addressString)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as ERC20Token

        if (contractAddress != other.contractAddress) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + contractAddress.hashCode()
        return result
    }
}