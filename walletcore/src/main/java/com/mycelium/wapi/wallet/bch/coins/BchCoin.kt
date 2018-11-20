package com.mycelium.wapi.wallet.bch.coins

import com.mrd.bitlib.model.Address
import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wapi.wallet.AddressUtils
import com.mycelium.wapi.wallet.GenericAddress
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.families.Families
import com.mycelium.wapi.wallet.exceptions.AddressMalformedException

abstract class BchCoin: CryptoCurrency(){
    init {
        family = Families.BITCOIN
    }

    @Throws(AddressMalformedException::class)
    override fun newAddress(addressStr: String): GenericAddress {
        return AddressUtils.from(this, addressStr)
    }

    override fun getName(): String {
        return "Bitcoin Cash"
    }

    override fun isMineAddress(address: String): Boolean {
        try {
            return Address.fromString(address, NetworkParameters.productionNetwork) != null
        } catch (ex: Exception) {
            return false
        }
    }
}