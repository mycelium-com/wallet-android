package com.mrd.bitlib.crypto.schnorr

/**
 * A data object representing a Schnorr Signature of `message`.
 *
 *
 * Note: `s` and `e` represent *positive* big-Endian integers. In particular,
 * any zero byte padding inserted by java.math.BigInteger should be removed before creating
 * one of these objects.
 *
 * @param s       the *s* parameter of the Schnorr signature.
 * @param e       the *e* parameter of the Schnorr signature.
 * @param message the message which has been signed.
 */
class SchnorrSignature(val s: ByteArray, val e: ByteArray, val message: ByteArray) {
     fun getSignatureBytes() = s + e
 }