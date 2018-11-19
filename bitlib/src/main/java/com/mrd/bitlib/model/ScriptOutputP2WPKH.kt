package com.mrd.bitlib.model

import java.io.Serializable

/**
 * Native SegWit pay-to-witness-public-key-hash script output
 */
class ScriptOutputP2WPKH : ScriptOutput, Serializable {
    private val addressBytes: ByteArray

    constructor(chunks: Array<ByteArray>, scriptBytes: ByteArray) : super(scriptBytes) {
        addressBytes = chunks[1]
    }

    constructor(scriptBytes: ByteArray) : super(scriptBytes) {
        addressBytes = scriptBytes.copyOfRange(2, scriptBytes.size)
    }

    override fun getAddressBytes() = addressBytes

    override fun getAddress(network: NetworkParameters): Address =
            SegwitAddress(network, 0x00, addressBytes)

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

