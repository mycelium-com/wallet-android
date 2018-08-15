/*
*******************************************************************************    
*   Ledger Bitcoin Hardware Wallet Java API
*   (c) 2014-2015 Ledger - 1BTChip7VfTnrPra5jqci7ejnMguuHogTn
*   
*  Licensed under the Apache License, Version 2.0 (the "License");
*  you may not use this file except in compliance with the License.
*  You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*  See the License for the specific language governing permissions and
*   limitations under the License.
********************************************************************************
*/

package com.mrd.bitlib.util

import kotlin.experimental.and

@Throws(IllegalArgumentException::class)
fun compressPublicKey(publicKey: ByteArray): ByteArray {
    when (publicKey[0]) {
        0x04.toByte() -> if (publicKey.size != 65) {
            throw IllegalArgumentException("Invalid public key")
        }
        0x02.toByte(), 0x03.toByte() -> {
            if (publicKey.size != 33) {
                throw IllegalArgumentException("Invalid public key")
            }
            return publicKey
        }
        else -> throw IllegalArgumentException("Invalid public key")
    }
    val result = ByteArray(33)
    result[0] = if (publicKey[64].and(1) != 0.toByte()) {
        0x03.toByte()
    } else {
        0x02.toByte()
    }
    System.arraycopy(publicKey, 1, result, 1, 32)
    return result
}
