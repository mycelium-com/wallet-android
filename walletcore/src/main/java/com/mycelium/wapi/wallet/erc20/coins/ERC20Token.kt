package com.mycelium.wapi.wallet.erc20.coins

import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.families.Families
import com.mycelium.wapi.wallet.eth.EthAddress
import org.web3j.abi.datatypes.Address as W3jAddress
import org.web3j.crypto.WalletUtils

class ERC20Token(name: String = "", symbol: String = "", unitExponent: Int = 18, val contractAddress: String) : CryptoCurrency() {
    init {
        id = name
        family = Families.ETHEREUM
        this.name = name
        this.symbol = symbol
        this.unitExponent = unitExponent
    }

    override fun parseAddress(addressString: String): Address? =
            if (WalletUtils.isValidAddress(addressString)) {
                // additional wrap of addressString into Address is called upon
                // to unify addresses with and without '0x' prefix
                EthAddress(this, W3jAddress(addressString).toString())
            } else {
                null
            }
}