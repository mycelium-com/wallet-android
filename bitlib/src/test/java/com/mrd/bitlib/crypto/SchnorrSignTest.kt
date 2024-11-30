package com.mrd.bitlib.crypto

import com.mrd.bitlib.crypto.schnorr.SchnorrSign
import com.mrd.bitlib.crypto.schnorr.SchnorrVerify
import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.util.HexUtils
import org.junit.Assert
import org.junit.Test

class SchnorrSignTest {

    @Test
    fun testNumber1() {
        getPrivKey("10").run {
            val message = "hell".toByteArray()
            val signature = SchnorrSign(this.privateKey).sign(message)
            println("signature size" + signature.size)
            Assert.assertEquals(64, signature.size)
            val result = SchnorrVerify(this.publicKey).verify(signature, message)
            Assert.assertEquals(true, result)
        }
    }

    @Test
    fun testNumber2() {
        val x =
            HexUtils.toBytes("bed123a21c0e50b003d302e83e755a444cbd436dfc4ea6635696c49499e47da6") //, a private key
        val rand =
            HexUtils.toBytes("6dfb9c259dc3b79f03470418af01cb1e064692dacc353f0f656cad0bfec583a7") //, an ephemeral random value (supposed to change for every signature)
        val msg =
            HexUtils.toBytes("21fbd20b359eee7bfea88e837108be44a1a421e33a05a45bc832d3e1a7aa713a") // , the message being signed, aka the sighash
        val signResult =
            HexUtils.toBytes("42cbaa49d55d54b88e426a844d4f14b82f1969f78047edbcbcf67d24ac60005f7ee88356cc872f239b4add26a92d5e500b26430bc39037d18d63ad09506d6bfa")


        val signature = SchnorrSign(x)
            .sign(msg, rand)
        println(HexUtils.toHex(signature))
        println(HexUtils.toHex(signResult))

        Assert.assertArrayEquals(signature, signResult)
    }

    @Test
    fun testNumber3() {
        val secretKey =
            InMemoryPrivateKey(HexUtils.toBytes("0000000000000000000000000000000000000000000000000000000000000003"))

        val publicKey =
            HexUtils.toBytes("F9308A019258C31049344F85F89D5229B531C845836F99B08601F113BCE036F9")

        Assert.assertArrayEquals(
            PublicKey(HexUtils.toBytes("02") + publicKey).pubKeyCompressed,
            secretKey.publicKey.pubKeyCompressed
        )

        val auxRand =
            HexUtils.toBytes("0000000000000000000000000000000000000000000000000000000000000000")
        val message =
            HexUtils.toBytes("0000000000000000000000000000000000000000000000000000000000000000")
        val sign = HexUtils.toBytes(
            "E907831F80848D1069A5371B402410364BDF1C5F8307B0084C55F1CE2DCA821525F66A4A85EA8B71E482A74F382D2CE5EBEEE8FDB2172F477DF4900D310536C0"
        )
        val signGen = SchnorrSign(secretKey.privateKey).sign(message, auxRand)

        Assert.assertArrayEquals(sign, signGen)

        Assert.assertEquals(true, SchnorrVerify(publicKey).verify(signGen, message))
    }

    @Test
    fun testNumber4() {
        val privateKey =
            HexUtils.toBytes("0340034003400340034003400340034003400340034003400340034003400340")
        val publicKey =
            HexUtils.toBytes("778CAA53B4393AC467774D09497A87224BF9FAB6F6E68B23086497324D6FD117")
        val auxRand =
            HexUtils.toBytes("0000000000000000000000000000000000000000000000000000000000000000")
        val message =
            HexUtils.toBytes("99999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999")
        val sign =
            HexUtils.toBytes("403B12B0D8555A344175EA7EC746566303321E5DBFA8BE6F091635163ECA79A8585ED3E3170807E7C03B720FC54C7B23897FCBA0E9D0B4A06894CFD249F22367")

        val signGen = SchnorrSign(privateKey).sign(message, auxRand)
        Assert.assertArrayEquals(signGen, sign)
    }

    companion object {
        private val network: NetworkParameters = NetworkParameters.testNetwork

        /**
         * Helper to get defined public keys
         *
         * @param s one byte hex values as string representation. "00" - "ff"
         */
        private fun getPrivKey(s: String?): InMemoryPrivateKey =
            InMemoryPrivateKey(
                HexUtils.toBytes(s + "00000000000000000000000000000000000000000000000000000000000000"),
                true
            )
    }
}