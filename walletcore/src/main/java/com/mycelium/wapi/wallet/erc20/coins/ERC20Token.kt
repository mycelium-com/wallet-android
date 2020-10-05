package com.mycelium.wapi.wallet.erc20.coins

import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.families.Families
import com.mycelium.wapi.wallet.eth.coins.EthCoin

class ERC20Token(name: String = "", symbol: String = "", unitExponent: Int = 18, val contractAddress: String) : CryptoCurrency() {
    init {
        id = name
        family = Families.ETHEREUM
        this.name = name
        this.symbol = symbol
        this.unitExponent = unitExponent
        this.isUtxosBased = false
    }

    override fun parseAddress(addressString: String?): Address? = EthCoin.parseAddress(this, addressString)
}