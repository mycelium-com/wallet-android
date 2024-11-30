package com.mrd.bitlib.crypto.schnorr

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.crypto.PrivateKey
import com.mrd.bitlib.crypto.ec.EcTools
import com.mrd.bitlib.crypto.ec.Parameters
import com.mrd.bitlib.util.TaprootUtils
import com.mrd.bitlib.util.toByteArray
import java.math.BigInteger
import java.security.SecureRandom

//https://learnmeabitcoin.com/technical/cryptography/elliptic-curve/schnorr/
class SchnorrSign(val privateKey: BigInteger) {

    constructor(privateKeyArray: ByteArray) :
            this(BigInteger(1, privateKeyArray))

    private val random: SecureRandom = SecureRandom()

    /**
     * Sign `message` using the private key given to the constructor.
     *
     * @param message the message to be signed.
     * @return
     */

    @JvmOverloads
    fun sign(message: ByteArray, randomBytes: ByteArray? = null): ByteArray {
        val rand =
            if (randomBytes == null) ByteArray(32).apply { random.nextBytes(this) }
            else randomBytes

        val publicKeyPoint =
            EcTools.multiply(Parameters.G, privateKey)

        val d = if (publicKeyPoint.y.toBigInteger().testBit(0))
            Parameters.n - privateKey else privateKey
        val auxRandHash = TaprootUtils.hashAux(rand)
        val t = d xor BigInteger(1, auxRandHash)
        val k0 = BigInteger(
            1,
            TaprootUtils.hashNonce(t.toByteArray(32) + publicKeyPoint.x.toByteArray(32) + message)
        ).mod(Parameters.n)
        if (k0 == BigInteger.ZERO) throw Exception("nonce must not be zero (this is almost impossible, but checking anyway)")

        val R = EcTools.multiply(Parameters.G, k0)

        val k = if (R.y.toBigInteger().testBit(0)) Parameters.n - k0 else k0
        val e = BigInteger(
            1, TaprootUtils.hashChallenge(
                R.x.toByteArray(32) + publicKeyPoint.x.toByteArray(32) + message
            )
        ).mod(Parameters.n)

//        // Calculate s
        val s = (k + e * d).mod(Parameters.n)

        // Signature is (r || s)
        val result = R.x.toByteArray(32) + s.toByteArray(32)
        if (!SchnorrVerify(publicKeyPoint.x.toByteArray(32)).verify(result, message))
            throw Exception("verification failed")

        return result
    }

}