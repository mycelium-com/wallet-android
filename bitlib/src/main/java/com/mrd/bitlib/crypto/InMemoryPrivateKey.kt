package com.mrd.bitlib.crypto

import com.google.common.base.Optional
import com.google.common.base.Preconditions
import com.mrd.bitlib.bitcoinj.Base58
import com.mrd.bitlib.crypto.InMemoryPrivateKey.DsaSignatureNonceGen
import com.mrd.bitlib.crypto.InMemoryPrivateKey.DsaSignatureNonceGenDeterministic
import com.mrd.bitlib.crypto.ec.EcTools
import com.mrd.bitlib.crypto.ec.Parameters
import com.mrd.bitlib.crypto.ec.Point
import com.mrd.bitlib.crypto.schnorr.SchnorrSign
import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.util.ByteWriter
import com.mrd.bitlib.util.HashUtils
import com.mrd.bitlib.util.Sha256Hash
import com.mrd.bitlib.util.TaprootUtils
import com.mrd.bitlib.util.cutStartByteArray
import java.io.Serializable
import java.lang.IllegalArgumentException
import java.math.BigInteger
import java.util.Arrays

/**
 * A Bitcoin private key that is kept in memory.
 */
class InMemoryPrivateKey() : PrivateKey(), KeyExporter, Serializable {
    private lateinit var privateKey: BigInteger
    override lateinit var publicKey: PublicKey


    /**
     * Construct a random private key using a secure random source with optional
     * compressed public keys.
     *
     * @param  randomSource
     * The random source from which the private key will be
     * deterministically generated.
     * @param compressed
     * Specifies whether the corresponding public key should be
     * compressed
     */
    /**
     * Construct a random private key using a secure random source. Using this
     * constructor yields uncompressed public keys.
     */
    @JvmOverloads
    constructor(randomSource: RandomSource, compressed: Boolean = false) : this() {
        val nBitLength = Parameters.n.bitLength()
        var d: BigInteger
        do {
            // Make a BigInteger from bytes to ensure that Andriod and 'classic'
            // java make the same BigIntegers from the same random source with the
            // same seed. Using BigInteger(nBitLength, random)
            // produces different results on Android compared to 'classic' java.
            val bytes = ByteArray(nBitLength / 8)
            randomSource.nextBytes(bytes)
            bytes[0] = (bytes[0].toInt() and 0x7F).toByte() // ensure positive number
            d = BigInteger(bytes)
        } while (d == BigInteger.ZERO || (d.compareTo(Parameters.n) >= 0))

        var Q = EcTools.multiply(Parameters.G, d)
        privateKey = d
        if (compressed) {
            // Convert Q to a compressed point on the curve
            Q = Point(Q.getCurve(), Q.getX(), Q.getY(), true)
        }
        publicKey = PublicKey(Q.getEncoded())
    }

    constructor(hash: Sha256Hash, compressed: Boolean) : this(hash.bytes, compressed)

    /**
     * Construct from private key bytes. Using this constructor yields
     * uncompressed public keys.
     *
     * @param bytes
     * The private key as an array of bytes
     * @param compressed
     * Specifies whether the corresponding public key should be
     * compressed
     */
    /**
     * Construct from private key bytes. Using this constructor yields
     * uncompressed public keys.
     *
     * @param bytes
     * The private key as an array of bytes
     */
    @JvmOverloads
    constructor(bytes: ByteArray, compressed: Boolean = false) : this() {
        require(bytes.size == 32) { "The length must be 32 bytes" }
        // Ensure that we treat it as a positive number
        val keyBytes = ByteArray(33)
        System.arraycopy(bytes, 0, keyBytes, 1, 32)
        privateKey = BigInteger(keyBytes)
        var Q = EcTools.multiply(Parameters.G, privateKey)
        if (compressed) {
            // Convert Q to a compressed point on the curve
            Q = Point(Q.getCurve(), Q.getX(), Q.getY(), true)
        }
        publicKey = PublicKey(Q.getEncoded())
    }

    /**
     * Construct from private and public key bytes
     *
     * @param priBytes
     * The private key as an array of bytes
     */
    constructor(priBytes: ByteArray, pubBytes: ByteArray) : this() {
        require(priBytes.size == 32) { "The length of the array of bytes must be 32" }
        // Ensure that we treat it as a positive number
        val keyBytes = ByteArray(33)
        System.arraycopy(priBytes, 0, keyBytes, 1, 32)
        privateKey = BigInteger(keyBytes)
        publicKey = PublicKey(pubBytes)
    }

    /**
     * Construct from a base58 encoded key (SIPA format)
     */
    constructor(base58Encoded: String?, network: NetworkParameters) : this() {
        var decoded = Base58.decodeChecked(base58Encoded)

        // Validate format
        require(!(decoded == null || decoded.size < 33 || decoded.size > 34)) { "Invalid base58 encoded key" }
        require(!(network == NetworkParameters.productionNetwork && decoded[0] != 0x80.toByte())) { "The base58 encoded key is not for the production network" }
        require(!(network == NetworkParameters.testNetwork && decoded[0] != 0xEF.toByte())) { "The base58 encoded key is not for the test network" }

        // Determine whether compression should be used for the public key
        var compressed: Boolean
        if (decoded.size == 34) {
            require(decoded[33].toInt() == 0x01) { "Invalid base58 encoded key" }
            // Get rid of the compression indication byte at the end
            val temp = ByteArray(33)
            System.arraycopy(decoded, 0, temp, 0, temp.size)
            decoded = temp
            compressed = true
        } else {
            compressed = false
        }
        // Make positive and clear network prefix
        decoded[0] = 0

        privateKey = BigInteger(decoded)
        var Q = EcTools.multiply(Parameters.G, privateKey)
        if (compressed) {
            // Convert Q to a compressed point on the curve
            Q = Point(Q.getCurve(), Q.getX(), Q.getY(), true)
        }
        publicKey = PublicKey(Q.getEncoded())
    }

    private fun calculateE(n: BigInteger, messageHash: ByteArray): BigInteger {
        if (n.bitLength() > messageHash.size * 8) {
            return BigInteger(1, messageHash)
        } else {
            val messageBitLength = messageHash.size * 8
            var trunc = BigInteger(1, messageHash)

            if (messageBitLength - n.bitLength() > 0) {
                trunc = trunc.shiftRight(messageBitLength - n.bitLength())
            }

            return trunc
        }
    }


    private abstract class DsaSignatureNonceGen {
        abstract fun getNonce(): BigInteger
    }

    private class DsaSignatureNonceGenDeterministic(
        messageHash: Sha256Hash,
        privateKey: KeyExporter
    ) : DsaSignatureNonceGen() {
        private val messageHash: Sha256Hash
        private val privateKey: KeyExporter

        init {
            this.messageHash = messageHash
            this.privateKey = privateKey
        }

        // rfc6979 compliant generation of k-value for DSA
        override fun getNonce(): BigInteger {
            // Step b

            var v = ByteArray(32)
            Arrays.fill(v, 0x01.toByte())

            // Step c
            var k = ByteArray(32)
            Arrays.fill(k, 0x00.toByte())

            // Step d
            val bwD = ByteWriter(32 + 1 + 32 + 32)
            bwD.putBytes(v)
            bwD.put(0x00.toByte())
            bwD.putBytes(privateKey.getPrivateKeyBytes())
            bwD.putBytes(messageHash.getBytes())
            k = Hmac.hmacSha256(k, bwD.toBytes())

            // Step e
            v = Hmac.hmacSha256(k, v)

            // Step f
            val bwF = ByteWriter(32 + 1 + 32 + 32)
            bwF.putBytes(v)
            bwF.put(0x01.toByte())
            bwF.putBytes(privateKey.getPrivateKeyBytes())
            bwF.putBytes(messageHash.getBytes())
            k = Hmac.hmacSha256(k, bwF.toBytes())

            // Step g
            v = Hmac.hmacSha256(k, v)

            // Step H2b
            v = Hmac.hmacSha256(k, v)

            var t = bits2int(v)

            // Step H3, repeat until T is within the interval [1, Parameters.n - 1]
            while ((t.signum() <= 0) || (t.compareTo(Parameters.n) >= 0)) {
                val bwH = ByteWriter(32 + 1)
                bwH.putBytes(v)
                bwH.put(0x00.toByte())
                k = Hmac.hmacSha256(k, bwH.toBytes())
                v = Hmac.hmacSha256(k, v)

                t = BigInteger(v)
            }
            return t
        }

        fun bits2int(`in`: ByteArray): BigInteger {
            // Step H1/H2a, ignored as tlen === qlen (256 bit)
            return BigInteger(1, `in`)
        }
    }

    override fun generateSignature(messageHash: Sha256Hash): Signature =
        generateSignatureInternal(
            messageHash,
            DsaSignatureNonceGenDeterministic(messageHash, this)
        )


    private fun generateSignatureInternal(
        messageHash: Sha256Hash,
        kGen: DsaSignatureNonceGen
    ): Signature {
        val n = Parameters.n
        val e = calculateE(n, messageHash.getBytes()) //leaving strong typing here
        var r: BigInteger?
        var s: BigInteger?

        // 5.3.2
        do  // generate s
        {
            val k = kGen.getNonce()

            // generate r
            val p = EcTools.multiply(Parameters.G, k)

            // 5.3.3
            val x = p.getX().toBigInteger()

            r = x.mod(n)

            s = k.modInverse(n).multiply(e.add(privateKey.multiply(r))).mod(n)
        } while (s == BigInteger.ZERO)

        // Enforce low S value
        if (s.compareTo(Parameters.MAX_SIG_S) > 0) {
            // If the signature is larger than MAX_SIG_S, inverse it
            s = Parameters.n.subtract(s)
        }

        return Signature(r, s)
    }

    override fun getPrivateKeyBytes(): ByteArray {
        val result = ByteArray(32)
        val bytes = privateKey.toByteArray()
        if (bytes.size <= result.size) {
            System.arraycopy(bytes, 0, result, result.size - bytes.size, bytes.size)
        } else {
            // This happens if the most significant bit is set and we have an
            // extra leading zero to avoid a negative BigInteger
            Preconditions.checkState(bytes.size == 33 && bytes[0].toInt() == 0)
            System.arraycopy(bytes, 1, result, 0, bytes.size - 1)
        }
        return result
    }

    override fun getBase58EncodedPrivateKey(network: NetworkParameters): String =
        if (publicKey.isCompressed) {
            getBase58EncodedPrivateKeyCompressed(network)
        } else {
            getBase58EncodedPrivateKeyUncompressed(network)
        }

    private fun getBase58EncodedPrivateKeyUncompressed(network: NetworkParameters): String {
        val toEncode = ByteArray(1 + 32 + 4)
        // Set network
        toEncode[0] = if (network.isProdnet()) 0x80.toByte() else 0xEF.toByte()
        // Set key bytes
        val keyBytes = getPrivateKeyBytes()
        System.arraycopy(keyBytes, 0, toEncode, 1, keyBytes.size)
        // Set checksum
        val checkSum = HashUtils.doubleSha256(toEncode, 0, 1 + 32).firstFourBytes()
        System.arraycopy(checkSum, 0, toEncode, 1 + 32, 4)
        // Encode
        return Base58.encode(toEncode)
    }

    private fun getBase58EncodedPrivateKeyCompressed(network: NetworkParameters): String {
        val toEncode = ByteArray(1 + 32 + 1 + 4)
        // Set network
        toEncode[0] = if (network.isProdnet()) 0x80.toByte() else 0xEF.toByte()
        // Set key bytes
        val keyBytes = getPrivateKeyBytes()
        System.arraycopy(keyBytes, 0, toEncode, 1, keyBytes.size)
        // Set compressed indicator
        toEncode[33] = 0x01
        // Set checksum
        val checkSum = HashUtils.doubleSha256(toEncode, 0, 1 + 32 + 1).firstFourBytes()
        System.arraycopy(checkSum, 0, toEncode, 1 + 32 + 1, 4)
        // Encode
        return Base58.encode(toEncode)
    }

    override fun makeSchnorrBitcoinSignature(message: ByteArray, merkle: ByteArray): ByteArray =
        makeSchnorrBitcoinSignature(message, merkle, null)

    override fun makeSchnorrBitcoinSignature(
        message: ByteArray,
        merkle: ByteArray,
        auxRand: ByteArray?
    ): ByteArray {
        val tweak = TaprootUtils.tweak(publicKey, merkle)
        return SchnorrSign(TaprootUtils.tweakPrivateKey(this.getPrivateKeyBytes(), tweak))
            .sign(message, auxRand)
    }

    companion object {
        private const val serialVersionUID = 1L

        @JvmStatic
        fun fromBase58String(
            base58: String?,
            network: NetworkParameters
        ): Optional<InMemoryPrivateKey?> {
            try {
                val key = InMemoryPrivateKey(base58, network)
                return Optional.of<InMemoryPrivateKey?>(key)
            } catch (e: IllegalArgumentException) {
                return Optional.absent<InMemoryPrivateKey?>()
            }
        }

        fun fromBase58MiniFormat(
            base58: String?,
            network: NetworkParameters?
        ): Optional<InMemoryPrivateKey?> {
            // Is it a mini private key on the format proposed by Casascius?
            if (base58 == null || base58.length < 2 || !base58.startsWith("S")) {
                return Optional.absent<InMemoryPrivateKey?>()
            }
            // Check that the string has a valid checksum
            val withQuestionMark = base58 + "?"
            val checkHash = HashUtils.sha256(withQuestionMark.toByteArray()).firstFourBytes()
            if (checkHash[0].toInt() != 0x00) {
                return Optional.absent<InMemoryPrivateKey?>()
            }
            // Now get the Sha256 hash and use it as the private key
            val privateKeyBytes = HashUtils.sha256(base58.toByteArray())
            try {
                val key = InMemoryPrivateKey(privateKeyBytes, false)
                return Optional.of<InMemoryPrivateKey?>(key)
            } catch (e: IllegalArgumentException) {
                return Optional.absent<InMemoryPrivateKey?>()
            }
        }
    }
}
