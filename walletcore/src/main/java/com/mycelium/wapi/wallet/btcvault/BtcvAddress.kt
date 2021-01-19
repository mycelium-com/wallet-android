package com.mycelium.wapi.wallet.btcvault

import com.mrd.bitlib.model.BitcoinAddress
import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import java.util.*

class BtcvAddress(override val coinType: CryptoCurrency,
                  address: BitcoinAddress) : BitcoinAddress(address.allAddressBytes), Address {


    override fun getBytes(): ByteArray {
        return allAddressBytes
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val that = o as BtcvAddress
        return allAddressBytes.contentEquals(that.allAddressBytes)
    }

    override fun hashCode(): Int {
        return Objects.hash(allAddressBytes)
    }

    override fun getSubType(): String = type.name

    override fun getNetwork(): NetworkParameters? {
        if (matchesNetwork(BTCVNetworkParameters.productionNetwork, version)) {
            return NetworkParameters.productionNetwork
        }
        if (matchesNetwork(BTCVNetworkParameters.testNetwork, version)) {
            return NetworkParameters.testNetwork
        }
        throw IllegalStateException("unknown network")
    }

    private fun matchesNetwork(network: NetworkParameters, version: Byte): Boolean {
        return (network.standardAddressHeader and 0xFF).toByte() == version || (network.multisigAddressHeader and 0xFF).toByte() == version
    }


}