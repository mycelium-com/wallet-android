package com.mrd.bitlib.crypto

import com.mrd.bitlib.crypto.schnorr.SchnorrVerify
import com.mrd.bitlib.util.HexUtils
import org.junit.Assert
import org.junit.Test

class SchnorrVerifyTest {

    @Test
    fun testNumber1() {
        val publicKey =
            PublicKey(HexUtils.toBytes("6c7711c517ee9d48b4183b56a4a62e8eec48fd997c053dd6358488db6264a520"))
        val message =
            HexUtils.toBytes("ca9642c5cb262877cb2d3fe87a2346e1a7507d96b84bae7b2dfb4b65282597da")
        val signature =
            HexUtils.toBytes("58df2ac9a7664759447951af46ef0e6043227c0dd7cf2bd68a838315dfc5956c726cb6d25f5c3c5d9f709740d08b0abca481e349d5aff1c8346b00e187a6022c")

        Assert.assertEquals(true, SchnorrVerify(publicKey).verify(signature, message))
    }

    @Test
    fun testNumber2() {
        val publicKey =
            PublicKey(HexUtils.toBytes("dff1d77f2a671c5f36183726db2341be58feae1da2deced843240f7b502ba659"))
        val message =
            HexUtils.toBytes("243f6a8885a308d313198a2e03707344a4093822299f31d0082efa98ec4e6c89")
        val signature =
            HexUtils.toBytes("6896bd60eeae296db48a229ff71dfe071bde413e6d43f917dc8dcf8c78de33418906d11ac976abccb20b091292bff4ea897efcb639ea871cfa95f6de339e4b0a")

        Assert.assertEquals(true, SchnorrVerify(publicKey).verify(signature, message))
    }
}