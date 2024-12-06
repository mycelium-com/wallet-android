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
            val signature = SchnorrSign(this.privateKeyBytes).sign(message)
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

    data class Case(
        val secretKey: ByteArray,
        val publicKey: ByteArray,
        val auxRand: ByteArray,
        val message: ByteArray,
        val signature: ByteArray
    )

    val testVectors = listOf(
        Case(
            HexUtils.toBytes("0000000000000000000000000000000000000000000000000000000000000003"),
            HexUtils.toBytes("F9308A019258C31049344F85F89D5229B531C845836F99B08601F113BCE036F9"),
            HexUtils.toBytes("0000000000000000000000000000000000000000000000000000000000000000"),
            HexUtils.toBytes("0000000000000000000000000000000000000000000000000000000000000000"),
            HexUtils.toBytes(
                "E907831F80848D1069A5371B402410364BDF1C5F8307B0084C55F1CE2DCA821525F66A4A85EA8B71E482A74F382D2CE5EBEEE8FDB2172F477DF4900D310536C0"
            )
        ),
        Case(
            HexUtils.toBytes("B7E151628AED2A6ABF7158809CF4F3C762E7160F38B4DA56A784D9045190CFEF"),
            HexUtils.toBytes("DFF1D77F2A671C5F36183726DB2341BE58FEAE1DA2DECED843240F7B502BA659"),
            HexUtils.toBytes("0000000000000000000000000000000000000000000000000000000000000001"),
            HexUtils.toBytes("243F6A8885A308D313198A2E03707344A4093822299F31D0082EFA98EC4E6C89"),
            HexUtils.toBytes(
                "6896BD60EEAE296DB48A229FF71DFE071BDE413E6D43F917DC8DCF8C78DE33418906D11AC976ABCCB20B091292BFF4EA897EFCB639EA871CFA95F6DE339E4B0A"
            )
        ),
        Case(
            HexUtils.toBytes("C90FDAA22168C234C4C6628B80DC1CD129024E088A67CC74020BBEA63B14E5C9"),
            HexUtils.toBytes("DD308AFEC5777E13121FA72B9CC1B7CC0139715309B086C960E18FD969774EB8"),
            HexUtils.toBytes("C87AA53824B4D7AE2EB035A2B5BBBCCC080E76CDC6D1692C4B0B62D798E6D906"),
            HexUtils.toBytes("7E2D58D8B3BCDF1ABADEC7829054F90DDA9805AAB56C77333024B9D0A508B75C"),
            HexUtils.toBytes(
                "5831AAEED7B44BB74E5EAB94BA9D4294C49BCF2A60728D8B4C200F50DD313C1BAB745879A5AD954A72C45A91C3A51D3C7ADEA98D82F8481E0E1E03674A6F3FB7"
            )
        ),
        Case(
            HexUtils.toBytes("0340034003400340034003400340034003400340034003400340034003400340"),
            HexUtils.toBytes("778CAA53B4393AC467774D09497A87224BF9FAB6F6E68B23086497324D6FD117"),
            HexUtils.toBytes("0000000000000000000000000000000000000000000000000000000000000000"),
            HexUtils.toBytes(""),
            HexUtils.toBytes(
                "71535DB165ECD9FBBC046E5FFAEA61186BB6AD436732FCCC25291A55895464CF6069CE26BF03466228F19A3A62DB8A649F2D560FAC652827D1AF0574E427AB63"
            )
        ),
        Case(
            HexUtils.toBytes("0340034003400340034003400340034003400340034003400340034003400340"),
            HexUtils.toBytes("778CAA53B4393AC467774D09497A87224BF9FAB6F6E68B23086497324D6FD117"),
            HexUtils.toBytes("0000000000000000000000000000000000000000000000000000000000000000"),
            HexUtils.toBytes("11"),
            HexUtils.toBytes(
                "08A20A0AFEF64124649232E0693C583AB1B9934AE63B4C3511F3AE1134C6A303EA3173BFEA6683BD101FA5AA5DBC1996FE7CACFC5A577D33EC14564CEC2BACBF"
            )
        ),
        Case(
            HexUtils.toBytes("0340034003400340034003400340034003400340034003400340034003400340"),
            HexUtils.toBytes("778CAA53B4393AC467774D09497A87224BF9FAB6F6E68B23086497324D6FD117"),
            HexUtils.toBytes("0000000000000000000000000000000000000000000000000000000000000000"),
            HexUtils.toBytes("0102030405060708090A0B0C0D0E0F1011"),
            HexUtils.toBytes(
                "5130F39A4059B43BC7CAC09A19ECE52B5D8699D1A71E3C52DA9AFDB6B50AC370C4A482B77BF960F8681540E25B6771ECE1E5A37FD80E5A51897C5566A97EA5A5"
            )
        ),
        Case(
            HexUtils.toBytes("0340034003400340034003400340034003400340034003400340034003400340"),
            HexUtils.toBytes("778CAA53B4393AC467774D09497A87224BF9FAB6F6E68B23086497324D6FD117"),
            HexUtils.toBytes("0000000000000000000000000000000000000000000000000000000000000000"),
            HexUtils.toBytes("99999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999"),
            HexUtils.toBytes(
                "403B12B0D8555A344175EA7EC746566303321E5DBFA8BE6F091635163ECA79A8585ED3E3170807E7C03B720FC54C7B23897FCBA0E9D0B4A06894CFD249F22367"
            )
        )

    )

    @Test
    fun testVectors() {
        testVectors.forEach {
            testVector(it)
        }
    }

    fun testVector(testCase: Case) {
        val secretKey = InMemoryPrivateKey(testCase.secretKey)
        val publicKey = testCase.publicKey

        Assert.assertArrayEquals(
            PublicKey(HexUtils.toBytes("02") + publicKey).pubKeyCompressed,
            secretKey.publicKey.pubKeyCompressed
        )

        val auxRand = testCase.auxRand
        val message = testCase.message
        val sign = testCase.signature

        val signGen = SchnorrSign(secretKey.privateKeyBytes).sign(message, auxRand)
        Assert.assertArrayEquals(sign, signGen)
        Assert.assertEquals(true, SchnorrVerify(publicKey).verify(signGen, message))
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