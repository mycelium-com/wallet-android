package com.mrd.bitlib.model

import com.mrd.bitlib.bitcoinj.Base58

/**
 * https://github.com/bitcoin/bips/blob/master/bip-0141.mediawiki
 */
@Suppress("ClassName")
class P2SH_P2WPKHAddress(bytes: ByteArray) : Address(bytes) {

    companion object {
        @JvmStatic
        fun fromString(address: String, network: NetworkParameters): P2SH_P2WPKHAddress? {
            val addr = fromString(address) ?: return null
            return if (!addr.isValidAddress(network)) {
                null
            } else addr
        }

        /**
         * @param address string representation of an address
         * @return an Address if address could be decoded with valid checksum and length of 21 bytes
         * null else
         */
        @JvmStatic
        fun fromString(address: String?): P2SH_P2WPKHAddress? {
            if (address == null) {
                return null
            }
            if (address.isEmpty()) {
                return null
            }
            val bytes = Base58.decodeChecked(address)
            return if (bytes == null || bytes.size != NUM_ADDRESS_BYTES) {
                null
            } else P2SH_P2WPKHAddress(bytes)
        }

        @Throws(IllegalArgumentException::class)
        @JvmStatic
        fun fromBytes(address: ByteArray, network: NetworkParameters): P2SH_P2WPKHAddress {
            if (address.size != 20) {
                throw IllegalArgumentException("Address must be 20 bytes")
            }
            val all = byteArrayOf((network.multisigAddressHeader and 0xFF).toByte()).plus(address)
            return P2SH_P2WPKHAddress(all)
        }
    }
}