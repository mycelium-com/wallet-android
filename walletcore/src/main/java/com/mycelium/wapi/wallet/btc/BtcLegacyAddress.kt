package com.mycelium.wapi.wallet.btc

import com.mrd.bitlib.model.Address
import com.mrd.bitlib.model.AddressType
import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wapi.wallet.coins.BitcoinMain
import com.mycelium.wapi.wallet.coins.BitcoinTest
import com.mycelium.wapi.wallet.coins.CryptoCurrency

class BtcLegacyAddress(override val coinType: CryptoCurrency, bytes: ByteArray) : BtcAddress {

    val address: Address = Address(bytes)

    override val id = 0L

    override val bip32Path = address.bip32Path

    override val type: AddressType
        get() {
            val networkParameters: NetworkParameters
            if (coinType is BitcoinTest)
                networkParameters = NetworkParameters.testNetwork
            else if (coinType is BitcoinMain)
                networkParameters = NetworkParameters.productionNetwork
            else
                networkParameters = NetworkParameters.regtestNetwork

            return if (address.isP2SH(networkParameters)) {
                AddressType.P2SH_P2WPKH
            } else {
                AddressType.P2PKH
            }
        }


    override fun toString(): String {
        return address.toString()
    }

}
