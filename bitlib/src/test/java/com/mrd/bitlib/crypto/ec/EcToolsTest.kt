package com.mrd.bitlib.crypto.ec

import org.bouncycastle.jce.ECNamedCurveTable
import org.junit.Assert
import org.junit.Test
import java.math.BigInteger


class EcToolsTest {

    data class TestCase(
        val scalar: String,
        val outPointX: String,
        val outPointY: String
    )

    val testVectors = listOf<TestCase>(
        TestCase(
            "1",
            "55066263022277343669578718895168534326250603453777594175500187360389116729240",
            "32670510020758816978083085130507043184471273380659243275938904335757337482424"
        ),
        TestCase(
            "123",
            "74901340345789065325870760596348306623878342739272826068162779513906431781301",
            "14607169553442007236852410049041684566594265431374316230317606814245957553771"
        ),
        TestCase(
            "123456890123456789123456890123456890123456890",
            "62780742554487378524245722410201167461505548730785281269062986356704893305385",
            "79293367521434015225245197473078322310576067403117337405850452835031726199543"
        ),
        TestCase(
            "12345689012345678912345689012345689012345689001234568900123456890012345689001234",
            "36450769889213599056928860715341952519652740267820654172204261626013854573923",
            "41906472091748731017244842776304479062049301936755971311475968212961048469884"
        ),
        TestCase(
            "69882697060768559974272477832487912714723377361712700280297795074591808265888",
            "86491035528315154095401034694330835821040902558974468611435256849691732182074",
            "60188737031622463183480430386240837298057978569415504114230525964812703308639"
        )
    )

    @Test
    fun test1() {
        val Gx = BigInteger("79BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798", 16)
        val Gy = BigInteger("483ADA7726A3C4655DA4FBFC0E1108A8FD17B448A68554199C47D08FFB10D4B8", 16)
        val bouncyCurve = ECNamedCurveTable.getParameterSpec("secp256k1").curve
        val G = bouncyCurve.createPoint(Gx, Gy)

        testVectors.forEachIndexed { i, it ->
            val m = BigInteger(it.scalar)
            val result = measureTime("multiply $i") {
                Parameters.G.multiply(m)
            }
            Assert.assertEquals(
                "test(${i}) x",
                it.outPointX.toBigInteger(),
                result.x.toBigInteger()
            )
            Assert.assertEquals(
                "test(${i}) y",
                it.outPointY.toBigInteger(),
                result.y.toBigInteger()
            )

            val result2 = measureTime("multiply bouncy $i") {
                G.multiply(m).normalize()
            }

            Assert.assertEquals(
                "test bouncy(${i}) x",
                it.outPointX.toBigInteger(),
                result2.xCoord.toBigInteger()
            )
            Assert.assertEquals(
                "test bouncy(${i}) y",
                it.outPointY.toBigInteger(),
                result2.yCoord.toBigInteger()
            )
        }
    }

    fun <R> measureTime(tag: String, call: () -> R): R {
        val startTime = System.currentTimeMillis()
        val result = call()
        val endTime = System.currentTimeMillis()
        println("!!!! tag = $tag  time = ${endTime - startTime}")
        return result
    }
}