package com.mrd.bitlib.crypto

import com.mrd.bitlib.util.Sha256Hash

interface BitcoinSigner {
    fun makeStandardBitcoinSignature(transactionSigningHash: Sha256Hash): ByteArray

    fun makeSchnorrBitcoinSignature(
        message: ByteArray,
        merkle: ByteArray
    ): ByteArray

    fun makeSchnorrBitcoinSignature(
        message: ByteArray,
        merkle: ByteArray,
        auxRand: ByteArray?
    ): ByteArray
}
