package com.mrd.bitlib.crypto

import com.google.common.base.Preconditions
import com.mrd.bitlib.crypto.Gf256v2.Share
import java.math.BigInteger
import java.util.Random

/**
 * Implementation no limit
 */
class Gf256v2 @JvmOverloads constructor(polynomial: Int = DEFAULT_POLYNOMIAL) {
    companion object {
        const val DEFAULT_POLYNOMIAL: Int = 0x11d

        const val COEFF_BYTE_COUNT = 6
        const val INFINITY_BYTE_COUNT = 12
        private val INFINITY = BigInteger(
            1,
            ByteArray(INFINITY_BYTE_COUNT) { 0xf }
        )
    }

    /**
     * A share of a secret
     */
    data class Share(
        /**
         * The index of this share 0 < index < 256
         */
        val index: Int,
        /**
         * The content of this share
         */
        val data: ByteArray
    )

    private fun coefficients(secret: BigInteger, m: Int): List<BigInteger> {
        val random = Random()
        val coefficients = mutableListOf<BigInteger>(secret)
        coefficients.addAll(
            (0..m - 2).map {
                BigInteger(1, ByteArray(COEFF_BYTE_COUNT).apply { random.nextBytes(this) })
            }
        )
        return coefficients
    }

    // Function to calculate y = f(x) for the polynomial
    fun evaluatePolynomial(
        coefficients: List<BigInteger>, x: BigInteger, prime: BigInteger
    ): BigInteger =
        coefficients.foldRightIndexed(BigInteger.ZERO) { index, coef, acc ->
            acc + x.pow(index) * coef
        }

    private fun makeShare(i: Int, coefficients: List<BigInteger>, prime: BigInteger): Share {
        val x = i.toBigInteger()
        val y = evaluatePolynomial(coefficients, x, prime)
        return Share(i, y.toByteArray())
    }

    /**
     * Combine a list of shares into a secret.
     *
     *
     * If the number of shares does not exactly match the original threshold that
     * the shares was created with, then the generated secret is not correct.
     *
     * @param shares the shares to combine
     * @return the combined secret
     */
    fun combineShares(sharesList: List<Share>, prime: BigInteger = INFINITY): ByteArray {
        require(sharesList.size >= 2) { "At least two shares are needed to reconstruct the secret" }
        val shares = sharesList.map { it.index.toBigInteger() to BigInteger(it.data) }

        val secret = shares.fold(BigInteger.ZERO) { acc, (xj, yj) ->

            val numerator = shares.filter { it.first != xj }
                .fold(BigInteger.ONE) { num, (xi, _) -> (num * xi.negate()) /*% prime*/ }

            val denominator = shares.filter { it.first != xj }
                .fold(BigInteger.ONE) { den, (xi, _) -> (den * (xj - xi)) /*% prime*/ }

//            val lagrange = numerator / denominator.modInverse(prime) % prime

            val lagrange = numerator / denominator
            (acc + yj * lagrange) /*% prime*/
        }
        return (secret /*% prime*/).toByteArray()
    }

    /**
     * Shard a secret into a number of shares in such a way that only the
     * specified threshold number of shares can recreate the secret.
     *
     * @param secret    the secret to shard
     * @param threshold the number of shares needed to recreate the secret
     * @param shares    the number of shares to create
     * @return a list of shares where only the specified threshold number of
     * shares can recreate the secret
     */
    fun makeShares(secret: ByteArray, threshold: Int, totalShares: Int): List<Share> =
        makeShares(BigInteger(1, secret), threshold, totalShares)

    fun makeShares(
        secret: BigInteger,
        threshold: Int,
        totalShares: Int,
        prime: BigInteger = INFINITY
    ): List<Share> {
        Preconditions.checkArgument(totalShares > 0, "Number of shares must be larger than zero")
        Preconditions.checkArgument(
            threshold <= totalShares,
            "Number of shares needed must be less than or equal to the number of shares"
        )
        val coefficients = coefficients(secret, threshold)
        return (1..totalShares).map { i ->
            makeShare(i, coefficients, prime)
        }
    }

}
