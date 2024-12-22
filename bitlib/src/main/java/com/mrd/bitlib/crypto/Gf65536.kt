package com.mrd.bitlib.crypto

import com.google.common.base.Preconditions
import com.mrd.bitlib.util.ByteWriter
import com.mrd.bitlib.util.HashUtils
import java.lang.RuntimeException
import kotlin.math.min

/**
 * Implementation of a Galois Field (2^16)
 */
class Gf65536() {
    /**
     * A share of a secret
     */
    data class Share(
        /**
         * The index of this share 0 < index < 65535
         */
        val index: Int,
        /**
         * The content of this share
         */
        val data: ByteArray
    )


    private fun log(n: Int): Int = _logTable[n]


    private fun exp(n: Int): Int = _expTable[n]


    /**
     * Addition. This is a simple X-or of two byte arrays
     */
    private fun add(a: IntArray, b: IntArray): IntArray {
        Preconditions.checkState(a.size == b.size)
        val c = IntArray(a.size)
        for (i in a.indices) {
            c[i] = add(a[i], b[i])
        }
        return c
    }

    /**
     * Addition. This is a simple X-or of two bytes
     */
    private fun add(a: Int, b: Int): Int = a xor b

    /**
     * Substitution, same as addition
     */
    private fun sub(a: Int, b: Int): Int = add(a, b)

    /**
     * Multiplication.
     */
    private fun mul(a: IntArray, b: IntArray): IntArray {
        require(a.size == b.size, { "a(${a.size}) and b(${b.size}) must have the same length" })
        val c = IntArray(a.size)
        for (i in a.indices) {
            c[i] = mul(a[i].toInt(), b[i].toInt())
        }
        return c
    }

    /**
     * Multiplication.
     */
    private fun mul(a: Int, b: Int): Int =
        if (a.toInt() == 0 || b == 0) {
            0
        } else {
            // The log of the product is the sum of the log of the multiplicands
            // modulo 2^16 - 1
            val lp = modModule(log(a) + log(b))
            exp(lp)
        }

    /**
     * Division.
     */
    private fun div(a: Int, b: Int): Int {
        if (b.toInt() == 0) {
            throw RuntimeException("Division by zero")
        }
        return if (a.toInt() == 0) {
            0
        } else {
            val lp = modModule(log(a) - log(b))
            exp(lp)
        }
    }

    private fun modModule(n: Int): Int = n % MODULO + (if (n < 0) MODULO else 0)


    fun sha256Coefficients(secret: ByteArray, m: Int): Array<IntArray> {
        val res = arrayOfNulls<IntArray>(m)
        var coeff = secret.toGFIntArray()
        res[0] = coeff
        for (n in 1 until m) {
            val writer = ByteWriter(coeff.size * 2 + 32)
            var i = 0
            while (i < coeff.size * 2) {
                val toHash: Int = min(32, coeff.size * 2 - i)
                writer.putBytes(HashUtils.sha256(coeff.toGFByteArray(), i, toHash).bytes)
                i += 32
            }
            coeff = writer.toBytes().toGFIntArray().copyOfRange(0, coeff.size)
            res[n] = coeff
        }
        return res.filterNotNull().toTypedArray()
    }

    fun makeShare(x: Int, coeff: Array<IntArray>): Share {
        Preconditions.checkArgument(x != 0)
        val q = coeff[0].size
        var s = coeff[0]

        var xpow: Int = 1

        for (i in 1 until coeff.size) {
            xpow = mul(xpow, x)
            s = add(s, mul(IntArray(q) { xpow }, coeff[i]))
        }
        return Share(x, s.toGFByteArray())
    }


    /**
     * Combine a list of shares into a secret.
     *
     *
     * If the number of shares does not exactly match the original threshold that
     * the shares was created with, then the generated secret is not correct.
     *
     * @param shares
     * the shares to combine
     * @return the combined secret
     */
    fun combineShares(shares: List<Share>): ByteArray {
        val m = shares.size
        Preconditions.checkArgument(m > 0)
        val firstData = shares.first().data.toGFIntArray()
        val q = firstData.size

        var n: Int = shares.fold(1) { n, share -> mul(n, share.index) }

        val a = shares.fold(IntArray(q)) { a, share ->
            var lc = div(n, share.index)
            for (otherShare in shares) {
                if (otherShare.index != share.index) {
                    lc = div(lc, sub(otherShare.index, share.index))
                }
            }
            add(a, mul(share.data.toGFIntArray(), IntArray(q) { lc }))
        }
        return a.toGFByteArray()
    }

    /**
     * Shard a secret into a number of shares in such a way that only the
     * specified threshold number of shares can recreate the secret.
     *
     * @param secret
     * the secret to shard
     * @param threshold
     * the number of shares needed to recreate the secret
     * @param shares
     * the number of shares to create
     * @return a list of shares where only the specified threshold number of
     * shares can recreate the secret
     */
    fun makeShares(secret: ByteArray, threshold: Int, shares: Int): List<Share> {
        Preconditions.checkArgument(shares > 0, "Number of shares must be larger than zero")
        Preconditions.checkArgument(
            threshold <= shares,
            "Number of shares needed must be less than or equal to the number of shares"
        )
        val coeff = sha256Coefficients(secret, threshold)
        val shareList = (1..shares).map { i ->
            makeShare(i, coeff)
        }
        return shareList
    }

    companion object {
        const val DEFAULT_POLYNOMIAL = 0x1100bL

        private const val INFINITY = 0xffff //65535
        private const val MODULO = INFINITY

        /*
        * Create a Galois Field with the default polynomial 0x1100b
        */

        private val _logTable = IntArray(65536)
        private val _expTable = IntArray(65536)

        init {
            // Initialize log and exponent tables by computing b = 2**i in GF
            // sequentially for all i from 0 to 254
            var b = 1 // 2**0
            for (i in 0..65534) {
                _logTable[b.toInt()] = i
                _expTable[i] = b.toInt()
                b = b shl 1
                if ((b and 0x10000) > 0) {
                    b = b xor DEFAULT_POLYNOMIAL.toInt()
                }
            }
            _logTable[0] = INFINITY.toByte().toInt()
            _expTable[INFINITY] = 0
            // Check that this polynomial really generates a GF by checking that we
            // are back to square one
            Preconditions.checkState(b == 1)
        }
    }
}

fun ByteArray.toGFIntArray(): IntArray {
    val data = if (size % 2 != 0) {
        val expanded = ByteArray(this.size + 1)
        this.copyInto(expanded, 1)
    } else {
        this
    }

    return IntArray(data.size / 2) { i ->
        val high = data[i * 2].toInt() and 0xFF // High byte (most significant byte)
        val low = data[i * 2 + 1].toInt() and 0xFF // Low byte (least significant byte)
        (high shl 8) or low // Combine the two bytes into a 16-bit integer
    }
}

fun IntArray.toGFByteArray(): ByteArray {
    return ByteArray(size * 2) { i ->
        val intValue = this[i / 2]
        if (i % 2 == 0) {
            (intValue shr 8).toByte() // Extract high byte
        } else {
            (intValue and 0xFF).toByte() // Extract low byte
        }
    }
}