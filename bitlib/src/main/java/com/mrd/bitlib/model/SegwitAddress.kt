package com.mrd.bitlib.model

import com.mrd.bitlib.bitcoinj.Base58
import com.mrd.bitlib.model.Script.OP_0

class SegwitAddress : Address {
    constructor(bytes: ByteArray) : super(bytes)
    constructor(bytes: ByteArray, stringAddress: String) : super(bytes, stringAddress)

    companion object {
        fun fromString(address: String, network: NetworkParameters): SegwitAddress? {
            val addr = SegwitAddress.fromString(address) ?: return null
            return if (!addr.isValidAddress(network)) {
                null
            } else addr
        }

        /**
         * @param address string representation of an address
         * @return an Address if address could be decoded with valid checksum and length of 21 bytes
         * null else
         */
        fun fromString(address: String?): SegwitAddress? {
            if (address == null) {
                return null
            }
            if (address.isEmpty()) {
                return null
            }
            val bytes = Base58.decodeChecked(address)
            return if (bytes == null || bytes.size != NUM_ADDRESS_BYTES) {
                null
            } else SegwitAddress(bytes)
        }

        @Throws(IllegalArgumentException::class)
        fun fromP2SHBytes(address: ByteArray, network: NetworkParameters): SegwitAddress {
            if (address.size != 20) {
                throw IllegalArgumentException("Address must be 20 bytes")
            }
            val all = byteArrayOf((network.multisigAddressHeader and 0xFF).toByte()).plus(address)
            return SegwitAddress(all)
        }
    }
}