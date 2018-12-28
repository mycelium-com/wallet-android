package com.mrd.bitlib.crypto

import com.mrd.bitlib.util.HexUtils.toBytes
import org.junit.Test

import org.junit.Assert.*

class PublicKeyTest {
    @Test
    fun getQ() {
        // TODO: Implement actual tests. This "test" only tests if kotlin lazy delegates work or so.
        val pk = PublicKey(toBytes("03ad1d8e89212f0b92c74d23bb710c00662ad1470198ac48c43f7d6f93a2a26873"))
        assertTrue(pk.Q.isCompressed)
    }
}
