package com.mycelium.wapi.wallet.btcvault

import com.mrd.bitlib.crypto.PublicKey
import com.mrd.bitlib.model.AddressType
import com.mrd.bitlib.model.BitcoinAddress
import com.mrd.bitlib.model.BtcvSegwitAddress
import com.mrd.bitlib.model.Script
import com.mrd.bitlib.model.hdpath.HdKeyPath
import com.mrd.bitlib.util.BitUtils
import com.mrd.bitlib.util.ByteReader
import com.mrd.bitlib.util.ByteWriter
import com.mrd.bitlib.util.HashUtils
import com.mycelium.wapi.wallet.CommonNetworkParameters
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.manager.AddressFactory


class BtcvAddressFactory(val coinType: CryptoCurrency, val networkParameters: CommonNetworkParameters) : AddressFactory<BtcvAddress> {

    override fun getAddress(publicKey: PublicKey, addressType: AddressType): BtcvAddress? =
            publicKey.toAddress(coinType, networkParameters, addressType)

    override fun bytesToAddress(bytes: ByteArray, path: HdKeyPath?): BtcvAddress? {
        return try {
            val reader = ByteReader(bytes)
            // Address bytes
            reader.getBytes(21)
            // Read length encoded string
            val addressString = String(reader.getBytes(reader.get().toInt()))
            val address = BtcvAddress.fromString(coinType, addressString)
            address?.bip32Path = path
            address
        } catch (e: ByteReader.InsufficientBytesException) {
            throw RuntimeException(e)
        }
    }

    override fun addressToBytes(address: BtcvAddress): ByteArray {
        val writer = ByteWriter(1024)
        // Add address as bytes
        writer.putBytes(address.allAddressBytes)
        // Add address as string
        val addressString = address.toString()
        writer.put(addressString.length.toByte())
        writer.putBytes(addressString.toByteArray())
        return writer.toBytes()
    }


    fun PublicKey.toAddress(coinType: CryptoCurrency, networkParameters: CommonNetworkParameters, addressType: AddressType, ignoreCompression: Boolean = false): BtcvAddress? {
        return when (addressType) {
            AddressType.P2PKH -> toP2PKHAddress(coinType, networkParameters)
            AddressType.P2SH_P2WPKH -> toNestedP2WPKH(coinType, networkParameters, ignoreCompression)
            AddressType.P2WPKH -> toP2WPKH(coinType, networkParameters, ignoreCompression)
            AddressType.P2TR -> TODO("not supported")
        }
    }

    private fun PublicKey.toP2PKHAddress(coinType: CryptoCurrency, network: CommonNetworkParameters): BtcvAddress? {
        if (publicKeyHash.size != 20) {
            return null
        }
        val all = ByteArray(BitcoinAddress.NUM_ADDRESS_BYTES)
        all[0] = (network.getStandardAddressHeader() and 0xFF).toByte()
        System.arraycopy(publicKeyHash, 0, all, 1, 20)
        return BtcvAddress(coinType, all)
    }

    private fun PublicKey.toNestedP2WPKH(coinType: CryptoCurrency, networkParameters: CommonNetworkParameters, ignoreCompression: Boolean = false): BtcvAddress? {
        if (ignoreCompression || isCompressed) {
            val hashedPublicKey = pubKeyHashCompressed
            val prefix = byteArrayOf(Script.OP_0.toByte(), hashedPublicKey.size.toByte())
            return fromP2SHBytes(coinType, HashUtils.addressHash(
                    BitUtils.concatenate(prefix, hashedPublicKey)), networkParameters)
        }
        throw IllegalStateException("Can't create segwit address from uncompressed key")
    }

    fun fromP2SHBytes(coinType: CryptoCurrency, bytes: ByteArray, network: CommonNetworkParameters): BtcvAddress? {
        if (bytes.size != 20) {
            return null
        }
        val all = ByteArray(BitcoinAddress.NUM_ADDRESS_BYTES)
        all[0] = (network.getMultisigAddressHeader() and 0xFF).toByte()
        System.arraycopy(bytes, 0, all, 1, 20)
        return BtcvAddress(coinType, all)
    }

    fun PublicKey.toP2WPKH(coinType: CryptoCurrency, networkParameters: CommonNetworkParameters, ignoreCompression: Boolean = false): BtcvSegwitAddress =
            if (ignoreCompression || isCompressed) {
                BtcvSegwitAddress(coinType, networkParameters, 0x00, HashUtils.addressHash(pubKeyCompressed))
            } else {
                throw IllegalStateException("Can't create segwit address from uncompressed key")
            }
}