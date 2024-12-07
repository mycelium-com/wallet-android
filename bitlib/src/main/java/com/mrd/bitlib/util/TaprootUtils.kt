package com.mrd.bitlib.util

import com.mrd.bitlib.crypto.PublicKey
import com.mrd.bitlib.crypto.ec.EcTools
import com.mrd.bitlib.crypto.ec.FieldElement
import com.mrd.bitlib.crypto.ec.Parameters
import com.mrd.bitlib.crypto.ec.Point
import java.math.BigInteger


class TaprootUtils {
    companion object {
        fun liftX(publicKey: Point): Point =
            liftX(publicKey.x.toBigInteger())

        fun liftX(x: BigInteger): Point {
            // check that x coordinate is less than the field size
            if (x >= Parameters.p) {
                throw Exception("x value in public key is not a valid coordinate because it is not less than the elliptic curve field size (x >= p)")
            }
            val y_sq =
                (x.modPow(3.toBigInteger(), Parameters.p) + 7.toBigInteger()).mod(Parameters.p)
            val y0 =
                y_sq.modPow((Parameters.p + BigInteger.ONE).divide(4.toBigInteger()), Parameters.p)
            // verify that the computed y value is the square root of y_sq (otherwise the public key was not a valid x coordinate on the curve)
            if (y0.modPow(2.toBigInteger(), Parameters.p) != y_sq) {
                throw RuntimeException("public key is not a valid x coordinate on the curve (y_sq != y ^ 2 mod p)")
            }
            // if the calculated y value is odd, negate it to get the even y value instead (for this x-coordinate)
            val y = if (y0.testBit(0)) Parameters.p - y0 else y0
            return Parameters.curve.createPoint(x, y, false)
        }

        fun taggedHash(tag: String, msg: ByteArray): Sha256Hash {
            val tagHash = HashUtils.sha256(tag.toByteArray()).bytes
            return HashUtils.sha256(tagHash + tagHash + msg)
        }

        fun hashTapTweak(msg: ByteArray): Sha256Hash =
            taggedHash("TapTweak", msg).apply {
                if (toPositiveBigInteger() >= Parameters.n)
                    throw RuntimeException("Tap Tweak >= Parameters.n")
            }

        fun hashAux(msg: ByteArray): Sha256Hash =
            taggedHash("BIP0340/aux", msg)

        fun hashNonce(msg: ByteArray): Sha256Hash =
            taggedHash("BIP0340/nonce", msg)

        fun hashChallenge(msg: ByteArray): Sha256Hash =
            taggedHash("BIP0340/challenge", msg)

        fun outputKey(internalKey: Point): ByteArray {
            val HTT = hashTapTweak(internalKey.x.toByteArray(32)).bytes
            return internalKey.add(Parameters.G.multiply(BigInteger(1, HTT))).x.toByteArray(32)
        }

        fun tweakedPrivateKey(privateKey: ByteArray, tweak: Sha256Hash): ByteArray {
//             seckey = seckey0 if has_even_y(P) else SECP256K1_ORDER - seckey0
            val privateKeyInt = BigInteger(1, privateKey)
            val P = EcTools.multiply(Parameters.G, privateKeyInt)
            val privateKeyNegated =
                if (P.y.toBigInteger().testBit(0)) Parameters.n - privateKeyInt else privateKeyInt
            return ((privateKeyNegated + tweak.toPositiveBigInteger()).mod(Parameters.n))
                .toByteArray(32)
        }

        fun sigHash(message: ByteArray): Sha256Hash =
            taggedHash("TapSighash", HexUtils.toBytes("00") + message)

        fun tweak(publicKey: PublicKey, merkle: ByteArray = ByteArray(0)): Sha256Hash =
            tweak(publicKey.pubKeyCompressed.cutStartByteArray(32), merkle)

        fun tweak(publicKey: ByteArray, merkle: ByteArray = ByteArray(0)): Sha256Hash =
            hashTapTweak(publicKey + merkle)

        fun tweakedPublicKey(publicKey: PublicKey, merkle: ByteArray = ByteArray(0)): ByteArray =
            tweakedPublicKey(publicKey.pubKeyCompressed.cutStartByteArray(32), merkle)

        fun tweakedPublicKey(publicKey: ByteArray, merkle: ByteArray = ByteArray(0)): ByteArray {
            val tweak = tweak(publicKey, merkle)
            val l = liftX(BigInteger(1, publicKey))
            val result = l.add(EcTools.multiply(Parameters.G, tweak.toPositiveBigInteger()))
            return result.x.toByteArray(32)
        }


//        fun scriptPubKey(internalKey: Point): ByteArray =
//                HexUtils.toBytes("5120") + outputKey(internalKey)

    }
}

fun FieldElement.toByteArray(destSize: Int): ByteArray =
    this.toBigInteger().toByteArray(destSize)

fun BigInteger.toByteArray(destSize: Int): ByteArray {
    val source = toByteArray()
    checkFirstZero(source, destSize)
    return source.cutStartByteArray(destSize)
}

fun ByteArray.cutStartByteArray(destSize: Int): ByteArray {
    val destOffset = if (destSize > this.size) destSize - this.size else 0
    val sourceOffset = if (this.size > destSize) this.size - destSize else 0
    return this.copyInto(ByteArray(destSize), destOffset, sourceOffset)
}

private fun checkFirstZero(source: ByteArray, destSize: Int) {
    if (source.size > destSize && source[0] != 0.toByte())
        throw RuntimeException("Wrong calculation")
}

