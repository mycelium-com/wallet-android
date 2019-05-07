/*
 * Copyright 2013 - 2018 Megion Research & Development GmbH
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

import com.mrd.bitlib.crypto.ec.Parameters
import com.mrd.bitlib.crypto.ec.Point
import com.mrd.bitlib.model.*
import com.mrd.bitlib.util.*

import java.io.Serializable
import java.util.*
import kotlin.experimental.and

class PublicKey(val publicKeyBytes: ByteArray) : Serializable {
    val pubKeyCompressed: ByteArray by lazy { compressPublicKey(publicKeyBytes) }
    val publicKeyHash: ByteArray by lazy { HashUtils.addressHash(publicKeyBytes) }
    val pubKeyHashCompressed: ByteArray by lazy { HashUtils.addressHash(pubKeyCompressed) }
    val Q: Point by lazy { Parameters.curve.decodePoint(publicKeyBytes) }

    /**
     * Is this a compressed public key?
     */
    val isCompressed: Boolean
        get() = Q.isCompressed

    /**
     * @param ignoreCompression allows deriving segwit addresses from uncompressed keys. This should
     * only be done to detect lost funds and under no circumstances should these addresses be shown
     * to the user.
     */
    @JvmOverloads
    fun toAddress(networkParameters: NetworkParameters, addressType: AddressType, ignoreCompression: Boolean = false): Address {
        return when (addressType) {
            AddressType.P2PKH -> toP2PKHAddress(networkParameters)
            AddressType.P2SH_P2WPKH -> toNestedP2WPKH(networkParameters, ignoreCompression)
            AddressType.P2WPKH -> toP2WPKH(networkParameters, ignoreCompression)
        }
    }

    @JvmOverloads
    fun getAllSupportedAddresses(networkParameters: NetworkParameters, ignoreCompression: Boolean = false) =
            SUPPORTED_ADDRESS_TYPES(isCompressed || ignoreCompression).map {
                it to toAddress(networkParameters, it, ignoreCompression)
            }.toMap()

    /**
     * @return [AddressType.P2SH_P2WPKH] address
     */
    private fun toNestedP2WPKH(networkParameters: NetworkParameters, ignoreCompression: Boolean = false): Address {
        if (ignoreCompression || isCompressed) {
            val hashedPublicKey = pubKeyHashCompressed
            val prefix = byteArrayOf(Script.OP_0.toByte(), hashedPublicKey.size.toByte())
            return Address.fromP2SHBytes(HashUtils.addressHash(
                    BitUtils.concatenate(prefix, hashedPublicKey)), networkParameters)
        }
        throw IllegalStateException("Can't create segwit address from uncompressed key")
    }

    /**
     * @return [AddressType.P2WPKH] address
     */
    private fun toP2WPKH(networkParameters: NetworkParameters, ignoreCompression: Boolean = false) : SegwitAddress =
            if (ignoreCompression || isCompressed) {
                SegwitAddress(networkParameters, 0x00, HashUtils.addressHash(pubKeyCompressed))
            } else {
                throw IllegalStateException("Can't create segwit address from uncompressed key")
            }

    /**
     * @return [AddressType.P2PKH] address
     */
    private fun toP2PKHAddress(networkParameters: NetworkParameters): Address =
            Address.fromStandardBytes(publicKeyHash, networkParameters)

    override fun hashCode(): Int {
        val bytes = publicKeyHash
        var hash = 0
        for (i in bytes.indices) {
            hash = (hash shl 8) + (bytes[i] and 0xff.toByte())
        }
        return hash
    }

    override fun equals(other: Any?): Boolean {
        if (other !is PublicKey) {
            return false
        }
        return Arrays.equals(publicKeyHash, other.publicKeyHash)
    }

    override fun toString(): String {
        return HexUtils.toHex(publicKeyBytes)
    }

    fun verifyStandardBitcoinSignature(data: Sha256Hash, signature: ByteArray, forceLowS: Boolean): Boolean {
        // Decode parameters r and s
        val reader = ByteReader(signature)
        val params = Signatures.decodeSignatureParameters(reader) ?: return false
        // Make sure that we have a hash type at the end
        if (reader.available() != HASH_TYPE) {
            return false
        }
        return if (forceLowS) {
            Signatures.verifySignatureLowS(data.bytes, params, Q)
        } else {
            Signatures.verifySignature(data.bytes, params, Q)
        }
    }

    // same as verifyStandardBitcoinSignature, but dont enforce the hash-type check
    fun verifyDerEncodedSignature(data: Sha256Hash, signature: ByteArray): Boolean {
        // Decode parameters r and s
        val reader = ByteReader(signature)
        val params = Signatures.decodeSignatureParameters(reader) ?: return false
        return Signatures.verifySignature(data.bytes, params, Q)
    }

    companion object {
        private const val serialVersionUID = 1L
        private const val HASH_TYPE = 1
        private fun SUPPORTED_ADDRESS_TYPES(isCompressed: Boolean) = if (isCompressed) {
            listOf(AddressType.P2PKH, AddressType.P2WPKH, AddressType.P2SH_P2WPKH
            )
        } else {
            // P2WPKH (and native P2WSH) do not allow uncompressed public keys as per
            // [BIP143](https://github.com/bitcoin/bips/blob/master/bip-0143.mediawiki#restrictions-on-public-key-type).
            // although we create addresses compressing the uncompressed key first, this is not
            // standard, so we don't show receiving addresses of this type and neither send change
            // there in order to maintain compatibility.
            listOf(AddressType.P2PKH)
        }
    }
}
