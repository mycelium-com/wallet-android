package com.mycelium.wapi.wallet.eth

import com.mycelium.wapi.wallet.GenericAddress
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import org.web3j.abi.datatypes.Address
import java.io.IOException
import java.io.ObjectInputStream
import java.lang.IllegalStateException


class EthAddress(cryptoCurrency: CryptoCurrency, val addressString: String) : GenericAddress {
    @Transient
    var address = Address(addressString)
    override val coinType = cryptoCurrency

    override fun getSubType() = "default"

    override fun getBytes() = address.toUint160().toString().toByteArray()

    override fun toString() = addressString

    /**
     * Always treat de-serialization as a full-blown constructor, by
     * validating the final state of the de-serialized object.
     */
    @Throws(ClassNotFoundException::class, IOException::class)
    private fun readObject(inputStream: ObjectInputStream) {
        //always perform the default de-serialization first
        inputStream.defaultReadObject()

        //make defensive copy of the mutable Date field
        address = Address(addressString)

        //ensure that object state has not been corrupted or tampered with maliciously
        if (address.toString() != addressString) {
            throw IllegalStateException("Unable to deserilize object")
        }
    }

    companion object {
        fun getDummyAddress(cryptoCurrency: CryptoCurrency) =
                EthAddress(cryptoCurrency, "0x000000000000000000000000000000000000dEaD")
    }
}