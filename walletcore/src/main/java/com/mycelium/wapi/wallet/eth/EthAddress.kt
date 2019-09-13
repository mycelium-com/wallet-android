package com.mycelium.wapi.wallet.eth

import com.mycelium.wapi.wallet.GenericAddress
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import org.web3j.abi.datatypes.Address

class EthAddress(cryptoCurrency: CryptoCurrency, addressString: String) : GenericAddress {
    val address = Address(addressString)
    override val coinType = cryptoCurrency

    override fun getSubType() = "default"

    override fun getBytes() = address.toUint160().toString().toByteArray()

    override fun toString() = address.toString()

    companion object {
        fun getDummyAddress(cryptoCurrency: CryptoCurrency) =
                EthAddress(cryptoCurrency, "0x000000000000000000000000000000000000dEaD")
    }
}