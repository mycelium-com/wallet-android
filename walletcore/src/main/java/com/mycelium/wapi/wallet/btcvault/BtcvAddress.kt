package com.mycelium.wapi.wallet.btcvault

import com.mrd.bitlib.bitcoinj.Base58
import com.mrd.bitlib.model.*
import com.mrd.bitlib.model.hdpath.HdKeyPath
import com.mrd.bitlib.util.BitUtils
import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.CommonNetworkParameters
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import java.util.*

open class BtcvAddress(override val coinType: CryptoCurrency,
                       addressBytes: ByteArray) : BitcoinAddress(addressBytes), Address {


    override fun getBytes(): ByteArray {
        return allAddressBytes
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val that = o as BtcvAddress
        return Arrays.equals(allAddressBytes, that.allAddressBytes)
    }

    override fun hashCode(): Int = Arrays.hashCode(allAddressBytes)

    override fun getSubType(): String = type.name

    override fun getNetwork(): NetworkParameters? {
        if (matchesNetwork(BTCVNetworkParameters.productionNetwork, version)) {
            return BTCVNetworkParameters.productionNetwork
        }
        if (matchesNetwork(BTCVNetworkParameters.testNetwork, version)) {
            return BTCVNetworkParameters.testNetwork
        }
        throw IllegalStateException("unknown network")
    }

    private fun matchesNetwork(network: NetworkParameters, version: Byte): Boolean =
            (network.standardAddressHeader and 0xFF).toByte() == version || (network.multisigAddressHeader and 0xFF).toByte() == version

    private var _bip32Path: HdKeyPath? = null

    override fun getBip32Path(): HdKeyPath? = _bip32Path

    override fun setBip32Path(bip32Path: HdKeyPath?) {
        _bip32Path = bip32Path
    }


    companion object {
        /**
         * @param address string representation of an address
         * @return an Address if address could be decoded with valid checksum and length of 21 bytes
         * null else
         */
        fun fromString(coinType: CryptoCurrency, address: String?): BtcvAddress? {
            if (address == null) {
                return null
            }
            if (address.isEmpty()) {
                return null
            }
            try {
                return BtcvSegwitAddress.decode(coinType, address)
            } catch (e: BtcvSegwitAddress.SegwitAddressException) {
                // this is not a SegWit address
            }
            val bytes = Base58.decodeChecked(address)
            return if (bytes == null || bytes.size != NUM_ADDRESS_BYTES) {
                null
            } else BtcvAddress(coinType, bytes)
        }

        fun getNullAddress(coinType: CryptoCurrency, network: CommonNetworkParameters, addressType: AddressType? = AddressType.P2PKH): BtcvAddress {
            val bytes = ByteArray(NUM_ADDRESS_BYTES)
            when (addressType) {
                AddressType.P2WPKH -> try {
                    return BtcvSegwitAddress(coinType, network, 0x00, BitUtils.copyOf(bytes, 20))
                } catch (ignore: SegwitAddress.SegwitAddressException) {
                }
                AddressType.P2SH_P2WPKH -> bytes[0] = (network.getMultisigAddressHeader() and 0xFF).toByte()
                else -> bytes[0] = (network.getStandardAddressHeader() and 0xFF).toByte()
            }
            return BtcvAddress(coinType, bytes)
        }
    }
}