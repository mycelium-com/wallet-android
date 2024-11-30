package com.mrd.bitlib.crypto.schnorr

import com.mrd.bitlib.crypto.PublicKey
import com.mrd.bitlib.crypto.ec.EcTools
import com.mrd.bitlib.crypto.ec.Parameters
import com.mrd.bitlib.crypto.ec.Point
import com.mrd.bitlib.util.TaprootUtils.Companion.hashChallenge
import com.mrd.bitlib.util.TaprootUtils.Companion.liftX
import com.mrd.bitlib.util.cutStartByteArray
import com.mrd.bitlib.util.toByteArray
import java.math.BigInteger


open class SchnorrVerify(val publicKey: Point) {

    constructor(publicKeyBytes: ByteArray) : this(liftX(BigInteger(1, publicKeyBytes)))

    constructor(publicKey: PublicKey) : this(liftX(BigInteger(1, publicKey.publicKeyBytes.cutStartByteArray(32))))

    fun verify(signature: ByteArray, message: ByteArray): Boolean {
        val P = publicKey

        val r = BigInteger(1, signature.copyOfRange(0, 32))
        val s = BigInteger(1, signature.copyOfRange(32, 64))

        if (r >= Parameters.n || s >= Parameters.n) {
            return false
        }

        val challenge = hashChallenge(r.toByteArray(32) + P.x.toByteArray(32) + message)
        val e = BigInteger(1, challenge).mod(Parameters.n)

        val point1 = EcTools.multiply(Parameters.G, s)
        val point2 = EcTools.multiply(P, Parameters.n - e)
        val R = point1.add(point2)

        if (R.y.toBigInteger().testBit(0)) {
            throw Exception("calculated R during signature verification has an odd y value (it should be even)")
        }

        return R.x.toBigInteger() == r
    }
}