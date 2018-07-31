package com.mrd.bitlib.model

import com.mrd.bitlib.bitcoinj.Base58
import com.mrd.bitlib.util.BitUtils
import com.mrd.bitlib.util.HashUtils
import java.util.*

/**
 * https://github.com/bitcoin/bips/blob/master/bip-0142.mediawiki#specification
 */
class P2WPKHAddress(bytes: ByteArray?) : Address(bytes) {
    override fun toString(): String {
        _address = Base58.encode(_bytes)
        return _address
    }

    companion object {
        @Throws(IllegalArgumentException::class)
        @JvmStatic
        fun fromBytes(address: ByteArray, witnessVersion: Byte, network: NetworkParameters): P2WPKHAddress {
            if (address.size != 20) {
                throw IllegalArgumentException("Address must be 20 bytes")
            }
            val prefix = byteArrayOf(network.p2WPKHAddressHeader.toByte(), witnessVersion, Script.OP_0.toByte())
            val payload = BitUtils.concatenate(prefix, address)
            val checksum = Arrays.copyOfRange((HashUtils.sha256(HashUtils.sha256(payload).bytes)).bytes, 0, 4)
            return P2WPKHAddress(BitUtils.concatenate(payload, checksum))
        }
    }
}