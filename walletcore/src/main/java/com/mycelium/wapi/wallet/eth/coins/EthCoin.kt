package com.mycelium.wapi.wallet.eth.coins

import com.mycelium.wapi.wallet.GenericAddress
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.families.Families
import com.mycelium.wapi.wallet.eth.EthAddress
import com.mycelium.wapi.wallet.exceptions.AddressMalformedException

abstract class EthCoin: CryptoCurrency() {
    init {
        family = Families.ETHEREUM
        unitExponent = 18
        addressPrefix = ""
    }

    override fun getName(): String {
        return "Ether"
    }

    @Throws(AddressMalformedException::class)
    override fun parseAddress(addressString: String): GenericAddress {
        return EthAddress(this, addressString)
    }
}