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

package com.mrd.bitlib.crypto

import java.math.BigInteger

import com.mrd.bitlib.crypto.ec.EcTools
import com.mrd.bitlib.crypto.ec.Point
import com.mrd.bitlib.util.HashUtils

/**
 * Elliptic curve Diffie-Hellman key exchange
 */
object Ecdh {
    const val SECRET_LENGTH = 32

    /**
     * Calculate a shared secret using the Elliptic curve variant of
     * Diffie-Hellman applied with Sha256
     *
     * @param foreignPublicKey
     * the public key of the other party
     * @param privateKey
     * you private key
     * @return a 32 byte shared secret
     */
    @JvmStatic
    fun calculateSharedSecret(foreignPublicKey: PublicKey, privateKey: InMemoryPrivateKey): ByteArray {
        val P = calculateSharedSecretPoint(foreignPublicKey, privateKey)
        val Px = P.x.toBigInteger()
        val bytes = EcTools.integerToBytes(Px, SECRET_LENGTH)
        return HashUtils.sha256(bytes).bytes
    }

    private fun calculateSharedSecretPoint(foreignPublicKey: PublicKey, privateKey: InMemoryPrivateKey): Point {
        val pk = BigInteger(1, privateKey.privateKeyBytes)
        return foreignPublicKey.Q.multiply(pk)
    }
}
