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

    constructor(scriptBytes: ByteArray) : super(scriptBytes) {
        addressBytes = scriptBytes
    }

    override fun getAddressBytes(): ByteArray {
        return addressBytes
    }

    override fun getAddress(network: NetworkParameters): Address {
        return SegwitAddress(network, 0x00, addressBytes)
    }

    companion object {
        private const val serialVersionUID = 1L

        @JvmStatic
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
