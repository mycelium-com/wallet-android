package com.mrd.bitlib.crypto.schnorr

import com.mrd.bitlib.crypto.PublicKey
import com.mrd.bitlib.crypto.ec.Curve
import com.mrd.bitlib.crypto.ec.EcTools
import com.mrd.bitlib.crypto.ec.Parameters
import com.mrd.bitlib.crypto.ec.Point
import com.mrd.bitlib.util.HashUtils
import com.mrd.bitlib.util.TaprootUtils
import com.mrd.bitlib.util.toByteArray
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


open class SchnorrVerify(val publicKey: Point) {

    constructor(publicKeyBytes: ByteArray) : this(lift_x(BigInteger(1, publicKeyBytes)))

    constructor(publicKey: PublicKey) : this(lift_x(BigInteger(1, publicKey.publicKeyBytes)))


    /**
     * Returns `true` iff `sig` verifies.
     *
     * @param sig the signature to verify.
     * @throws NoSuchAlgorithmException if `DIGEST_ALGORITHM` is not available.
     */

    fun verify(signature: ByteArray, message: ByteArray): Boolean {

        val P = publicKey //TaprootUtils.lift_x()

        val r = BigInteger(1, signature.copyOfRange(0, 32))
        val s = BigInteger(1, signature.copyOfRange(32, 64))
        if (r >= Parameters.n || s >= Parameters.n) return false

        val e = BigInteger(
            1,
            TaprootUtils.hashChallenge(r.toByteArray(32) + publicKey.x.toByteArray(32) + message)
        ).mod(Parameters.n)

        val point1 = EcTools.multiply(Parameters.G, s)
        val point2 = EcTools.multiply(P, Parameters.n - e)
        val R = point1.add(point2)

        if (R.y.toBigInteger().testBit(0))
            throw Exception("calculated R during signature verification has an odd y value (it should be even)")

        return R.x.toBigInteger() == r
    }

    companion object {
        fun lift_x(x: BigInteger): Point {
            //# use the elliptic curve equation (y² = x³+ax+b) to work out the value of y from x
            val y_sq = (x.pow(3) + 7.toBigInteger()).mod(Parameters.p)
            //# secp256k1 is chosen in a special way so that the square root of y is y^((p+1)/4)
            var y = y_sq.modPow(
                (Parameters.p + BigInteger.ONE).divide(4.toBigInteger()),
                Parameters.p
            )

//        # check that x coordinate is less than the field size
            if (x >= Parameters.p)
                throw Exception("x value in public key is not a valid coordinate because it is not less than the elliptic curve field size")

//        # verify that the computed y value is the square root of y_sq (otherwise the public key was not a valid x coordinate on the curve)
            if (y_sq != y.pow(2).mod(Parameters.p))
                throw Exception("public key is not a valid x coordinate on the curve")

//        # if the calculated y value is odd, negate it to get the even y value instead (for this x-coordinate)
            y = if (y.testBit(0)) Parameters.p - y else y
            return Parameters.curve.createPoint(x, y, false)
        }
    }
}