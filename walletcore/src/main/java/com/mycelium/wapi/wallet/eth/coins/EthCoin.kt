package com.mycelium.wapi.wallet.eth.coins

import com.mrd.bitlib.model.Address
import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wapi.wallet.AddressUtils
import com.mycelium.wapi.wallet.GenericAddress
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.families.Families
import com.mycelium.wapi.wallet.exceptions.AddressMalformedException

abstract class EthCoin: CryptoCurrency(){
    init {
        family = Families.ETHEREUM
        unitExponent = 8
        friendlyDigits = 2
        feeValue = value(1000)
    }

    @Throws(AddressMalformedException::class)
    override fun newAddress(addressStr: String): GenericAddress {
        return AddressUtils.from(this, addressStr)
    }
    override fun getSymbol() = "ETH"

    override fun getName(): String {
        return "Ethereum"
    }

    override fun isMineAddress(address: String): Boolean {
        return false
    }
}

object EthMain : EthCoin() {
    init {
        id = "ETH-Main"
        name = "Etherium"
    }
}


object EthTest : EthCoin() {
    init {
        id = "ETH-Test"
        name = "Ethereum Test"
    }
}