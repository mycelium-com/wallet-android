/*
 * Copyright 2013, 2014 Megion Research & Development GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mrd.bitlib.model

import java.io.Serializable

/**
 * TODO implemet with segwit or remove if redundant, don't merge with current state
 */
class ScriptOutputP2WSH : ScriptOutput, Serializable {
    //private val p2wshAddressBytes: ByteArray


    constructor(chunks: Array<ByteArray>, scriptBytes: ByteArray) : super(scriptBytes) {
        //      p2wshAddressBytes = chunks[1];
        throw NotImplementedError()
    }


    /**
     * Get the raw p2sh address that this output is for.
     *
     * @return The raw p2sh address that this output is for.
     */
    override fun getAddressBytes(): ByteArray {
        throw NotImplementedError()
        //return p2wshAddressBytes
    }

    override fun getAddress(network: NetworkParameters): Address {
        throw NotImplementedError()
        //      byte[] addressBytes = getAddressBytes();
        //      return Address.fromP2SHBytes(addressBytes, network);
    }

    companion object {
        private const val serialVersionUID = 1L

        // TODO test segwit
        fun isScriptOutputP2WSH(chunks: Array<ByteArray>): Boolean {
            if( chunks.isEmpty()) {
                return false
            }
            if (!Script.isOP(chunks[0], Script.OP_FALSE)) {
                return false
            }
            if (!Script.isOP(chunks[1], Script.OP_PUSH32)) {
                return false
            }
            return Script.scriptEncodeChunks(chunks).size == 34
        }
    }
}
