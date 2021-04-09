package com.mycelium.wapi.wallet.eth

import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import org.web3j.abi.datatypes.Address as E3jAddress
import java.io.IOException
import java.io.ObjectInputStream


class EthAddress(cryptoCurrency: CryptoCurrency, val addressString: String) : Address {
    @Transient
    var address = E3jAddress(addressString)
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

        address = E3jAddress(addressString)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is EthAddress) {
            return false
        }
        return getBytes().contentEquals(other.getBytes())
    }

    override fun hashCode(): Int {
        return getBytes().contentHashCode()
    }

    companion object {
        fun getDummyAddress(cryptoCurrency: CryptoCurrency) =
                EthAddress(cryptoCurrency, "0x000000000000000000000000000000000000dEaD")
    }
}