package com.mrd.bitlib.crypto.schnorr

import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom


class SchnorrSign(privateKey: ByteArray) :
        SchnorrVerify(G.modPow(BigInteger(Util.positiveIntToTwosCompliment(privateKey)), P)) {
    val privateKey = BigInteger(Util.positiveIntToTwosCompliment(privateKey))

    private val random: SecureRandom = SecureRandom()

    /**
     * Sign `message` using the private key given to the constructor.
     *
     * @param message the message to be signed.
     * @return
     * @throws NoSuchAlgorithmException thrown is `DIGEST_ALGORITHM` is not available.
     */
    fun sign(message: ByteArray): SchnorrSignature {
        return sign(message, BigInteger(Q.bitLength() - 1, random))
    }

    private fun sign(message: ByteArray, randomValue: BigInteger): SchnorrSignature {
        val m: MessageDigest = MessageDigest.getInstance(DIGEST_ALGORITHM)
        val r = G.modPow(randomValue, P)
        val rAsBytes = Util.twosComplimentToPositiveInt(r.toByteArray())
        val messageAndR = ByteArray(message.size + rAsBytes.size)
        System.arraycopy(message, 0, messageAndR, 0, message.size)
        System.arraycopy(rAsBytes, 0, messageAndR, message.size, rAsBytes.size)
        m.update(messageAndR)
        val e: ByteArray = m.digest()
        val positiveE = BigInteger(Util.positiveIntToTwosCompliment(e))
        val s = randomValue.subtract(privateKey.multiply(positiveE)).mod(Q)
        return SchnorrSignature(Util.twosComplimentToPositiveInt(s.toByteArray()), e, message)
    }

}