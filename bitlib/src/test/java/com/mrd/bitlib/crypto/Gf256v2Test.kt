package com.mrd.bitlib.crypto

import com.mrd.bitlib.util.HexUtils
import org.junit.Assert
import org.junit.Test
import java.math.BigInteger

class Gf256v2Test {
    val gf2 = Gf256v2()
    val gf = Gf256()
    val TEST_VECTORS = listOf(
        "123456789",
        "KwF3gnXvKvRyiPi6uR187K89kvAGh4oW7SeKFc2vKucuuhnNSZ1o",
    )

    @Test
    fun test() {
        TEST_VECTORS.forEach {
            val secret = it.toByteArray()
            val k = 50

            val shares = gf2.makeShares(secret, k, 300)

            val combined = gf2.combineShares(shares.subList(0, k))
            Assert.assertEquals(
                HexUtils.toHex(secret),
                HexUtils.toHex(combined)
            )
        }
    }

    @Test
    fun test1() {
        val secret = "123456789".toByteArray()

        val shares = gf2.makeShares(secret, 2, 3)
        val combined = gf2.combineShares(shares)
        Assert.assertEquals(
            HexUtils.toHex(secret),
            HexUtils.toHex(combined)
        )
    }

    @Test
    fun test2() {
        val secret = 1234.toBigInteger()
        val n = 6
        val k = 3

        val coeff2 = listOf<BigInteger>(secret, 166.toBigInteger(), 94.toBigInteger())

        (1..n).map { i ->
            val poly = gf2.evaluatePolynomial(coeff2, i.toBigInteger(), 255.toBigInteger())
        }

        val shares = gf2.makeShares(secret, k, n)
        val combine = gf2.combineShares(shares.subList(0, k))

        Assert.assertEquals(
            "test combine",
            secret,
            BigInteger(combine)
        )
    }

    @Test
    fun test3() {
        val secret = "123456789".toByteArray()
        val shares = gf.makeShares(secret, 2, 3)

        val combine = gf2.combineShares(shares.map { Gf256v2.Share(it.index.toInt(), it.data) })
        Assert.assertEquals(
            "test combine",
            String(secret),
            String(combine)
        )
    }
}
