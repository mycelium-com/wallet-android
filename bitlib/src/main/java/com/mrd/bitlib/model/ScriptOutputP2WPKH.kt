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
 * TODO implemet with segwit, don't merge with current state
 */
class ScriptOutputP2WPKH : ScriptOutput, Serializable {
    //private val p2pkhAddressBytes: ByteArray


    constructor(chunks: Array<ByteArray>, scriptBytes: ByteArray) : super(scriptBytes) {
        //      p2pkhAddressBytes = chunks[1];
        throw NotImplementedError()
    }

    /**
     * Get the raw p2sh address that this output is for.
     *
     * @return The raw p2sh address that this output is for.
     */
    override fun getAddressBytes(): ByteArray {
        throw NotImplementedError()
    }

    override fun getAddress(network: NetworkParameters): Address {
        throw NotImplementedError()
        //      byte[] addressBytes = getAddressBytes();
        //      return Address.fromP2SHBytes(addressBytes, network);
    }

    companion object {
        private const val serialVersionUID = 1L

        //TODO test
        fun isScriptOutputP2WPKH(chunks: Array<ByteArray>): Boolean {
            if (!Script.isOP(chunks[0], Script.OP_FALSE)) {
                return false
            }
            return chunks[1].size == 20
        }
    }
}
