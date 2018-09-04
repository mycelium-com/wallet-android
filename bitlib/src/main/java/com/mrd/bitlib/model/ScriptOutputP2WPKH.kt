package com.mrd.bitlib.model

import java.io.Serializable

/**
 * TODO implemet with segwit, don't merge with current state, bech
 */
class ScriptOutputP2WPKH : ScriptOutput, Serializable {
    private val addressBytes: ByteArray


    constructor(chunks: Array<ByteArray>, scriptBytes: ByteArray) : super(scriptBytes) {
        addressBytes = chunks[1]
    }

    override fun getAddressBytes(): ByteArray {
        return addressBytes
    }

    override fun getAddress(network: NetworkParameters): Address {
            return object : Address (addressBytes) {
            }
        //      byte[] addressBytes = getAddressBytes();
        //      return Address.fromP2SHBytes(addressBytes, network);
    }

    companion object {
        private const val serialVersionUID = 1L

        fun isScriptOutputP2WPKH(chunks: Array<ByteArray>): Boolean {
            if (chunks.isEmpty()) {
                return false
            }
            if (!Script.isOP(chunks[0], Script.OP_FALSE)) {
                return false
            }
            return chunks[1].size == 20
        }
    }
}
