package com.mycelium.wapi.fio

import com.mycelium.wapi.content.fio.isFioPublicKey
import org.junit.Test

class FioTestUtils {
    val validPublicKey = "FIO5ReMUvFM9X12eSuAR4QKjHsGJ6qponQP36xtV7WZLPBG35dJTr"
    val invalidPublicKey1 = "FIO5ReMUvFM9X12eSufe4QKjHsGJ6qponQP36xtV7WZLPBG35dJTr"
    val invalidPublicKey2 = "FIO5ReMUvFM9X12eSufe4QKjHsGJ6qponQP36xtWZLPBG35dJTr"
    @Test
    fun isValidPublicTest() {
        assert(validPublicKey.isFioPublicKey())
        assert(invalidPublicKey1.isFioPublicKey().not())
        assert(invalidPublicKey2.isFioPublicKey().not())
    }
}