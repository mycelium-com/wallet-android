package com.mrd.bitlib

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.crypto.PublicKey
import com.mrd.bitlib.util.HexUtils
import org.junit.Assert
import org.junit.Test


// https://github.com/bitcoin-core/btcdeb/blob/master/doc/tapscript-example-with-tap.md
//bc1p5cyxnuxmeuwuvkwfem96lqzszd02n6xdcjrs20cac6yqjjwudpxqkedrcr
//bc1pq0xg5j7xfkyhhhw9l0p0vu8h4zaqkwr80ygxeufz83hut47ddlq32dnksmj
//bc1pqyqszqgpqyqszqgpqyqszqgpqyqszqgpqyqszqgpqyqszqgpqyqsyjer9e

class TaprootTransactionTest {
    val privateKey = InMemoryPrivateKey(HexUtils.toBytes("cc8a4bc64d897bddc5fbc2f670f7a8ba0b386779106cf1223c6fc5d7cd6fc115"))
    val publicKey = PublicKey(HexUtils.toBytes("a60869f0dbcf1dc659c9cecbaf8050135ea9e8cdc487053f1dc6880949dc684c"))

//    val publicKey = PublicKey(HexUtils.toBytes("03ad1d8e89212f0b92c74d23bb710c00662ad1470198ac48c43f7d6f93a2a26873"))
//    val privateKey = InMemoryPrivateKey(HexUtils.toBytes("eb696a065ef48a2192da5b28b694f87544b30fae8327c4510137a922f32c6dcf"))

    @Test
    fun checkKeys() {
//        Assert.assertArrayEquals(publicKey.publicKeyBytes, privateKey.publicKey.publicKeyBytes)
        Assert.assertArrayEquals(publicKey.publicKeyBytes, privateKey.publicKey.pubKeyCompressed)
    }
}

