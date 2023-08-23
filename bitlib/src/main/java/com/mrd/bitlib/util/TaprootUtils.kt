package com.mrd.bitlib.util

import com.mrd.bitlib.crypto.ec.Parameters
import com.mrd.bitlib.crypto.ec.Point
import java.math.BigInteger


class TaprootUtils {
    companion object {
        fun lift_x(publicKey: Point): Point? {
            val x = publicKey.x.toBigInteger()
            if (x >= Parameters.p) {
                throw RuntimeException("x >= p")
            }
            val y_sq = (x.modPow(3.toBigInteger(), Parameters.p) + 7.toBigInteger()).mod(Parameters.p)
            val y = y_sq.modPow((Parameters.p + BigInteger.ONE).divide(4.toBigInteger()), Parameters.p)
            if (y.modPow(2.toBigInteger(), Parameters.p) != y_sq) {
                throw RuntimeException("y_sq != y ^ 2 mod p")
            }
            return Parameters.curve.createPoint(x, if (y.mod(2.toBigInteger()) == BigInteger.ZERO) y else Parameters.p - y, false)
        }

        fun taggedHash(tag: String, msg: ByteArray): ByteArray {
            val tagHash = HashUtils.sha256(tag.toByteArray()).bytes
            return HashUtils.sha256(tagHash + tagHash + msg).bytes
        }

        fun hashTapTweak(msg: ByteArray): ByteArray =
                taggedHash("TapTweak", msg)

        fun outputKey(internalKey: Point): ByteArray {
            val HTT = hashTapTweak(internalKey.x.toBigInteger().toByteArray()
                .let {
                    if (it.size > 32 && it[0] == 0.toByte()) it.copyOfRange(1, it.size) else it
                })
                .let {
                    if (it.size != 32) throw RuntimeException("Wrong HTT key calculation") else it
                }
            return internalKey.add(Parameters.G.multiply(BigInteger(1, HTT))).x.toBigInteger()
                .toByteArray()
                .let {
                    if (it.size > 32 && it[0] == 0.toByte()) it.copyOfRange(1, it.size) else it
                }
                .let {
                    if (it.size != 32) throw RuntimeException("Wrong outputKey calculation") else it
                }
        }

//        fun scriptPubKey(internalKey: Point): ByteArray =
//                HexUtils.toBytes("5120") + outputKey(internalKey)

    }
}
