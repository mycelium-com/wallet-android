package com.mrd.bitlib.crypto

import com.mrd.bitlib.model.NetworkParameters.productionNetwork
import org.junit.Assert
import org.junit.Test

class Bip38Test {
    @Test
    fun testVectorsFromTheBipEncrypt() = ENCRYPT_DECRYPT_TVS.forEach {
        val encoded = Bip38.encryptNoEcMultiply(it.passphrase, it.unencryptedWIF)
        Assert.assertEquals(it.encrypted, encoded)
        Assert.assertTrue(Bip38.isBip38PrivateKey(encoded))
    }

    @Test
    fun testVectorsFromTheBipDecrypt() = ENCRYPT_DECRYPT_TVS.forEach { testDecrypt(it) }

    @Test
    fun testVectorsForDecryptOnly() = DECRYPT_TVS.forEach { testDecrypt(it) }

    private fun testDecrypt(tv: TV) {
        Assert.assertEquals("Without Bom", tv.unencryptedWIF,
                Bip38.decrypt(tv.encrypted, tv.passphrase, productionNetwork))
        Assert.assertEquals("With Bom", tv.unencryptedWIF,
                Bip38.decrypt("\uFEFF" + tv.encrypted, tv.passphrase, productionNetwork))
    }

    private data class TV(val passphrase: String, val encrypted: String, val unencryptedWIF: String)

    companion object {
        // All Test Vectors from https://github.com/bitcoin/bips/blob/master/bip-0038.mediawiki#Test_vectors
        private val ENCRYPT_DECRYPT_TVS = arrayOf(
                // No compression, no EC multiply
                TV("TestingOneTwoThree", "6PRVWUbkzzsbcVac2qwfssoUJAN1Xhrg6bNk8J7Nzm5H7kxEbn2Nh2ZoGg", "5KN7MzqK5wt2TP1fQCYyHBtDrXdJuXbUzm4A9rKAteGu3Qi5CVR"),
                TV("Satoshi", "6PRNFFkZc2NZ6dJqFfhRoFNMR9Lnyj7dYGrzdgXXVMXcxoKTePPX1dWByq", "5HtasZ6ofTHP6HCwTqTkLDuLQisYPah7aUnSKfC7h4hMUVw2gi5"),
                TV("\u03D2\u0301\u0000" + StringBuilder().appendCodePoint(0x010400).appendCodePoint(0x01f4a9).toString(), "6PRW5o9FLp4gJDDVqJQKJFTpMvdsSGJxMYHtHaQBF3ooa8mwD69bapcDQn", "5Jajm8eQ22H3pGWLEVCXyvND8dQZhiQhoLJNKjYXk9roUFTMSZ4"),
                // Compression, no EC multiply
                TV("TestingOneTwoThree", "6PYNKZ1EAgYgmQfmNVamxyXVWHzK5s6DGhwP4J5o44cvXdoY7sRzhtpUeo", "L44B5gGEpqEDRS9vVPz7QT35jcBG2r3CZwSwQ4fCewXAhAhqGVpP"),
                TV("Satoshi", "6PYLtMnXvfG3oJde97zRyLYFZCYizPU5T3LwgdYJz1fRhh16bU7u6PPmY7", "KwYgW8gcxj1JWJXhPSu4Fqwzfhp5Yfi42mdYmMa4XqK7NJxXUSK7"))
        private val DECRYPT_TVS = arrayOf(
                //EC multiply, no compression, no lot/sequence numbers
                TV("TestingOneTwoThree", "6PfQu77ygVyJLZjfvMLyhLMQbYnu5uguoJJ4kMCLqWwPEdfpwANVS76gTX", "5K4caxezwjGCGfnoPTZ8tMcJBLB7Jvyjv4xxeacadhq8nLisLR2"),
                TV("Satoshi", "6PfLGnQs6VZnrNpmVKfjotbnQuaJK4KZoPFrAjx1JMJUa1Ft8gnf5WxfKd", "5KJ51SgxWaAYR13zd9ReMhJpwrcX47xTJh2D3fGPG9CM8vkv5sH"),
                // EC multiply, no compression, lot/sequence numbers
                TV("MOLON LABE", "6PgNBNNzDkKdhkT6uJntUXwwzQV8Rr2tZcbkDcuC9DZRsS6AtHts4Ypo1j", "5JLdxTtcTHcfYcmJsNVy1v2PMDx432JPoYcBTVVRHpPaxUrdtf8"),
                TV("\u039C\u039F\u039B\u03A9\u039D \u039B\u0391\u0392\u0395", "6PgGWtx25kUg8QWvwuJAgorN6k9FbE25rv5dMRwu5SKMnfpfVe5mar2ngH", "5KMKKuUmAkiNbA3DazMQiLfDq47qs8MAEThm4yL8R2PhV1ov33D"),
                // from https://github.com/mycelium-com/wallet-android/issues/581
                TV("123", "6PYU3LqtkXznTNiyvHw6ZRinCoaAKVF2k1Pfe9bHC1sATDNe6uzH3MLj7Z", "KxFMS25nh2rJBx1BpAaotL8ZDyKLuoUPWh25jUjtMpU2oSAZT8sQ"))
    }
}