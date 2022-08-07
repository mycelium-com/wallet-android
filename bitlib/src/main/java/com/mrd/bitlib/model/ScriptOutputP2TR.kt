package com.mrd.bitlib.model

import java.io.Serializable

/**
 * Native SegWit pay-to-taproot script output
 */
class ScriptOutputP2TR : ScriptOutput, Serializable {
    private val addressBytes: ByteArray

    constructor(chunks: Array<ByteArray>, scriptBytes: ByteArray) : super(scriptBytes) {
        addressBytes = chunks[1]
    }

    constructor(scriptBytes: ByteArray) : super(scriptBytes) {
        addressBytes = scriptBytes.copyOfRange(2, scriptBytes.size)
    }

    override fun getAddressBytes() = addressBytes

    override fun getAddress(network: NetworkParameters): BitcoinAddress =
            SegwitAddress(network, 0x01, addressBytes)

    companion object {
        private const val serialVersionUID = 1L

        @JvmStatic
        fun isScriptOutputP2TR(chunks: Array<ByteArray>): Boolean {
            if (chunks.size != 2) return false
            if (!Script.isOP(chunks[0], Script.OP_1)) return false
            return chunks[1].size == 32
        }
    }
}

