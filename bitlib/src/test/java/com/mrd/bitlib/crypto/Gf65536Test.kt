package com.mrd.bitlib.crypto

import com.mrd.bitlib.util.HexUtils
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

class Gf65536Test {
    val gf3 = Gf65536()

    data class TestCase(val secret: ByteArray, val threshold: Int, val shares: Int) {
        override fun toString(): String =
            "t=$threshold s=$shares ${String(secret)} "

    }

    val TEST_VECTORS = mutableListOf(
        TestCase("123456789".toByteArray(), 2, 3),
        TestCase("KwF3gnXvKvRyiPi6uR187K89kvAGh4oW7SeKFc2vKucuuhnNSZ1o".toByteArray(), 2, 3),
        TestCase(
            HexUtils.toBytes("77efbfbdd48b127056efbfbdefbfbdefbfbd5f756a2d39efbfbd5227efbfbdefbfbdefbfbdefbfbdefbfbd637befbfbd302eefbfbd6befbfbd"),
            257, 258
        )
    )

    @Before
    fun setUp() {
        val minShares = 255
        val maxShares = 260
        val random = Random(System.currentTimeMillis())
        TEST_VECTORS.addAll((0..1000).map {
            val total = random.nextInt(minShares, maxShares)
            val threshold = random.nextInt(2, total)
            val secret = random.nextBytes(32)
            TestCase(secret, threshold, total)
        })
    }

    @Test
    fun test() {
        TEST_VECTORS.forEachIndexed { i, it ->
            val secret = it.secret

            val shares = gf3.makeShares(secret, it.threshold, it.shares)

            var combined = gf3.combineShares(shares.subList(0, it.threshold))
            Assert.assertEquals(
                "test combine case=$i $it",
                HexUtils.toHex(secret.toGFIntArray().toGFByteArray()),
                HexUtils.toHex(combined)
            )
        }
    }

    @Test
    fun test_1() {
        val test = TEST_VECTORS[1]
        val coef = gf3.sha256Coefficients(test.secret, test.threshold)
        println("!!!! secret = ${HexUtils.toHex(test.secret)}")
        coef.forEach {
            println("!!!! coef = ${it.size} " + HexUtils.toHex(it.toGFByteArray()))
            Assert.assertEquals(
                "test size",
                coef.first().size,
                it.size
            )
        }


        val share = gf3.makeShare(1, coef)
        println("!!!! share = ${share.data.size} " + HexUtils.toHex(share.data))
    }
}
