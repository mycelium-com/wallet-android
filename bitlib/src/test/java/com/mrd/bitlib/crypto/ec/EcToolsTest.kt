package com.mrd.bitlib.crypto.ec

import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.math.ec.ECPoint
import org.junit.Assert
import org.junit.Test
import java.math.BigInteger

class EcToolsTest {

    data class TestCase(
        val inPointX: String,
        val inPointY: String,
        val scalar: String,
        val outPointX: String,
        val outPointY: String
    ) {
        fun getInPoint() =
            Parameters.curve.createPoint(BigInteger(inPointX), BigInteger(inPointY), false)

        fun getInPointv2(): ECPoint {
            val curveSpec = ECNamedCurveTable.getParameterSpec("secp256k1")
            return curveSpec.curve.createPoint(BigInteger(inPointX), BigInteger(inPointY))
        }

    }

    val testVectors = listOf<TestCase>(
        TestCase(
            "55066263022277343669578718895168534326250603453777594175500187360389116729240",
            "32670510020758816978083085130507043184471273380659243275938904335757337482424",
            "1",
            "55066263022277343669578718895168534326250603453777594175500187360389116729240",
            "32670510020758816978083085130507043184471273380659243275938904335757337482424"
        ),
        TestCase(
            "55066263022277343669578718895168534326250603453777594175500187360389116729240",
            "32670510020758816978083085130507043184471273380659243275938904335757337482424",
            "123",
            "74901340345789065325870760596348306623878342739272826068162779513906431781301",
            "14607169553442007236852410049041684566594265431374316230317606814245957553771"
        ),
        TestCase(
            "55066263022277343669578718895168534326250603453777594175500187360389116729240",
            "32670510020758816978083085130507043184471273380659243275938904335757337482424",
            "123456890123456789123456890123456890123456890",
            "62780742554487378524245722410201167461505548730785281269062986356704893305385",
            "79293367521434015225245197473078322310576067403117337405850452835031726199543"
        ),
        TestCase(
            "55066263022277343669578718895168534326250603453777594175500187360389116729240",
            "32670510020758816978083085130507043184471273380659243275938904335757337482424",
            "12345689012345678912345689012345689012345689001234568900123456890012345689001234",
            "36450769889213599056928860715341952519652740267820654172204261626013854573923",
            "41906472091748731017244842776304479062049301936755971311475968212961048469884"
        ),
        TestCase(
            "55066263022277343669578718895168534326250603453777594175500187360389116729240",
            "32670510020758816978083085130507043184471273380659243275938904335757337482424",
            "69882697060768559974272477832487912714723377361712700280297795074591808265888",
            "86491035528315154095401034694330835821040902558974468611435256849691732182074",
            "60188737031622463183480430386240837298057978569415504114230525964812703308639"
        )
    )

    @Test
    fun test1() {
        testVectors.forEachIndexed { i, it ->
            val point = it.getInPoint()
            val m = BigInteger(it.scalar)
            val result = measureTime("multiply $i") {
                point.multiply(m)
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

            val point2 = it.getInPointv2()
            val result2 = measureTime("multiply 2 $i") {
                point2.multiply(m)
            }
            Assert.assertEquals(
                "test 2(${i}) x",
                it.outPointX.toBigInteger(),
                result2.xCoord.toBigInteger()
            )
            Assert.assertEquals(
                "test 2(${i}) y",
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