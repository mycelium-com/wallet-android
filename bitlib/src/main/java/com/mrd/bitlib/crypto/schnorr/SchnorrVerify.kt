package com.mrd.bitlib.crypto.schnorr

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;



open class SchnorrVerify(val publicKey: BigInteger) {


//    fun SchnorrVerify(publicKey: ByteArray?) {
//        this.publicKey = BigInteger(Util.positiveIntToTwosCompliment(publicKey))
//    }

    /**
     * Provide a big-Endian signed integer representation of the public key.
     *
     */
    fun getPublicKey(): ByteArray? {
        return Util.twosComplimentToPositiveInt(publicKey.toByteArray())
    }

    /**
     * Returns `true` iff `sig` verifies.
     *
     * @param sig the signature to verify.
     * @throws NoSuchAlgorithmException if `DIGEST_ALGORITHM` is not available.
     */

    fun verify(sig: SchnorrSignature): Boolean {

        //r = (pow(self.g, s, self.p) * pow(self.publicKey, e, self.p)) % self.p
        val e = BigInteger(Util.positiveIntToTwosCompliment(sig.e))
        val s = BigInteger(Util.positiveIntToTwosCompliment(sig.s))
        val r: BigInteger = G.modPow(s, P).multiply(publicKey.modPow(e, P)).mod(P)
        val message: ByteArray = sig.message
        val rAsBytes: ByteArray = Util.twosComplimentToPositiveInt(r.toByteArray())
        val messageAndR = ByteArray(message.size + rAsBytes.size)
        System.arraycopy(message, 0, messageAndR, 0, message.size)
        System.arraycopy(rAsBytes, 0, messageAndR, message.size, rAsBytes.size)
        val m: MessageDigest = MessageDigest.getInstance(DIGEST_ALGORITHM)
        m.update(messageAndR)
        val newE: ByteArray = m.digest()

        //TODO(beresford): when validating the signature on the server for authentication, need
        //to check that the contents of the message is the one which is received.
        return e.equals(BigInteger(Util.positiveIntToTwosCompliment(newE)))
    }

    companion object {
        //Parameters cribbed from OpenSSL's J-PAKE implementation
        val P: BigInteger = BigInteger("fd7f53811d75122952df4a9c2eece4e7f611b7523cef4400c31e3f80b6512669455d402251fb593d8d58fabfc5f5ba30f6cb9b556cd7813b801d346ff26660b76b9950a5a49f9fe8047b1022c24fbba9d7feb7c61bf83b57e7c6a8a6150f04fb83f6d3c51ec3023554135a169132f675f3ae2b61d72aeff22203199dd14801c7", 16)
        val Q: BigInteger = BigInteger("9760508f15230bccb292b982a2eb840bf0581cf5", 16)
        val G: BigInteger = BigInteger("f7e1a085d69b3ddecbbcab5c36b857b97994afbbfa3aea82f9574c0b3d0782675159578ebad4594fe67107108180b449167123e84c281613b7cf09328cc8a6e13c167a8b547c8d28e0a3ae1e2bb3a675916ea37f0bfa213562f1fb627a01243bcca4f1bea8519089a883dfe15ae59f06928b665e807b552564014c3bfecf492a", 16)

        val DIGEST_ALGORITHM: String = "SHA-256"

    }
}