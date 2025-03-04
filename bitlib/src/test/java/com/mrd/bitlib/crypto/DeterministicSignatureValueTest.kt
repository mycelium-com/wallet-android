package com.mrd.bitlib.crypto

import com.mrd.bitlib.util.ByteReader
import com.mrd.bitlib.util.HashUtils
import com.mrd.bitlib.util.HexUtils
import org.junit.Assert
import org.junit.Test
import java.io.UnsupportedEncodingException
import java.math.BigInteger

class DeterministicSignatureValueTest {
    private inner class SignatureTestVector(
        val pk: PrivateKey, val message: ByteArray, val r: BigInteger, val s: BigInteger
    ) {
        constructor(pk: String?, message: String, r: String, s: String) : this(
            InMemoryPrivateKey(HexUtils.toBytes(pk), true),
            message.toByteArray(charset("UTF-8")),
            BigInteger(r),
            BigInteger(s)
        )

        constructor(pk: String?, message: String, signature: Signature) : this(
            InMemoryPrivateKey(HexUtils.toBytes(pk), true),
            message.toByteArray(charset("UTF-8")),
            signature.r,
            signature.s
        )

        constructor(pk: String?, message: String, signatureDer: String?) : this(
            pk, message,
            Signatures.decodeSignatureParameters(ByteReader(HexUtils.toBytes(signatureDer))),
        )


        fun check() {
            val toSign =
                HashUtils.sha256(message)
            val sig = pk.generateSignature(toSign)

            Assert.assertEquals("R", r, sig.r)
            Assert.assertEquals("S", s, sig.s)

            // Verify that the signature is valid
            Assert.assertTrue(Signatures.verifySignature(toSign.bytes, sig, pk.publicKey.Q))
        }
    }

    /**
     * Check if the signatures are RFC6979 compliant, by using various test vectors
     */
    @Test
    @Throws(UnsupportedEncodingException::class)
    fun checkDeterministicSig() {
        // TODO: more/other testvectors?
        // https://bitcointalk.org/index.php?topic=285142.40
        val vectors = arrayOf(
            SignatureTestVector(
                "0000000000000000000000000000000000000000000000000000000000000001",
                "Everything should be made as simple as possible, but not simpler.",
                "3044022033a69cd2065432a30f3d1ce4eb0d59b8ab58c74f27c41a7fdb5696ad4e6108c902206f807982866f785d3f6418d24163ddae117b7db4d5fdf0071de069fa54342262"
            ),
            SignatureTestVector(
                "fffffffffffffffffffffffffffffffebaaedce6af48a03bbfd25e8cd0364140",
                "Equations are more important to me, because politics is for the present, but an equation is something for eternity.",
                "3044022054c4a33c6423d689378f160a7ff8b61330444abb58fb470f96ea16d99d4a2fed022007082304410efa6b2943111b6a4e0aaa7b7db55a07e9861d1fb3cb1f421044a5"
            ),
            SignatureTestVector(
                "fffffffffffffffffffffffffffffffebaaedce6af48a03bbfd25e8cd0364140",
                "Not only is the Universe stranger than we think, it is stranger than we can think.",
                "3045022100ff466a9f1b7b273e2f4c3ffe032eb2e814121ed18ef84665d0f515360dab3dd002206fc95f5132e5ecfdc8e5e6e616cc77151455d46ed48f5589b7db7771a332b283"
            ),
            SignatureTestVector(
                "0000000000000000000000000000000000000000000000000000000000000001",
                "How wonderful that we have met with a paradox. Now we have some hope of making progress.",
                "3045022100c0dafec8251f1d5010289d210232220b03202cba34ec11fec58b3e93a85b91d3022075afdc06b7d6322a590955bf264e7aaa155847f614d80078a90292fe205064d3"
            ),
            SignatureTestVector(
                "69ec59eaa1f4f2e36b639716b7c30ca86d9a5375c7b38d8918bd9c0ebc80ba64",
                "Computer science is no more about computers than astronomy is about telescopes.",
                "304402207186363571d65e084e7f02b0b77c3ec44fb1b257dee26274c38c928986fea45d02200de0b38e06807e46bda1f1e293f4f6323e854c86d58abdd00c46c16441085df6"
            ),
            SignatureTestVector(
                "00000000000000000000000000007246174ab1e92e9149c6e446fe194d072637",
                "...if you aren't, at any given time, scandalized by code you wrote five or even three years ago, you're not learning anywhere near enough",
                "3045022100fbfe5076a15860ba8ed00e75e9bd22e05d230f02a936b653eb55b61c99dda48702200e68880ebb0050fe4312b1b1eb0899e1b82da89baa5b895f612619edf34cbd37"
            ),
            SignatureTestVector(
                "000000000000000000000000000000000000000000056916d0f9b31dc9b637f3",
                "The question of whether computers can think is like the question of whether submarines can swim.",
                "3045022100cde1302d83f8dd835d89aef803c74a119f561fbaef3eb9129e45f30de86abbf9022006ce643f5049ee1f27890467b77a6a8e11ec4661cc38cd8badf90115fbd03cef"
            ),  // own
            SignatureTestVector(
                "00fffffffffffffffffffffffffffffebaaedce6af48a03bbfd25e8cd0364140",
                "Equations are more important to me, because politics is for the present, but an equation is something for eternity.",
                "26820159967830081822798970710067426129483172706261789974922243430675895943943",
                "14912181929787960001668259713503722835850920746900933010493547455088626203371"
            )
        )
        vectors.forEachIndexed { i, stv ->
            println("step $i")
            stv.check()
        }
    }
}
