package fiofoundation.io.fiosdk.models

import fiofoundation.io.fiosdk.toHexString
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom

class Cryptography(val key:ByteArray,var iv:ByteArray?)
{
    private val encKey:ByteArray

    init {

        if(iv == null)
            iv = generateIv()

        encKey = if(key.size>32) key.copyOf(32) else key
    }

    companion object Static {
        val Algorithm = "AES"

        fun createHmac(data: ByteArray, key: ByteArray): ByteArray {
            val keySpec = SecretKeySpec(key, "HmacSHA256")
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(keySpec)

            val hmac = mac.doFinal(data)
            return hmac
        }
    }

    @Throws(Exception::class)
    fun encrypt(plainText: String): ByteArray
    {
        return encrypt(plainText.toByteArray(StandardCharsets.UTF_8))
    }

    @Throws(Exception::class)
    fun encrypt(data: ByteArray): ByteArray
    {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val keySpec = SecretKeySpec(this.encKey, "AES")

        val ivSpec = IvParameterSpec(iv)

        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)

        return cipher.doFinal(data)
    }

    @Throws(Exception::class)
    @ExperimentalUnsignedTypes
    fun encrypt(data: UByteArray): ByteArray
    {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val keySpec = SecretKeySpec(this.encKey, "AES")

        val ivSpec = IvParameterSpec(iv)

        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)

        return cipher.doFinal(data.asByteArray())
    }

    @Throws(Exception::class)
    fun encryptAsString(plainText: ByteArray): String
    {
        return String(encrypt(plainText),StandardCharsets.UTF_8)
    }


    @Throws(Exception::class)
    fun decrypt(encryptedText: String): ByteArray
    {
        return decrypt(encryptedText.toByteArray())
    }

    @Throws(Exception::class)
    fun decrypt(encryptedText: ByteArray): ByteArray
    {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val keySpec = SecretKeySpec(this.encKey, "AES")
        val ivSpec = IvParameterSpec(iv)

        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)

        return cipher.doFinal(encryptedText)
    }

    @Throws(Exception::class)
    fun decryptAsString(encryptedText: ByteArray): String
    {
        return String(decrypt(encryptedText),StandardCharsets.UTF_8)
    }

    fun getIVasHex():String
    {
        return iv!!.toHexString()
    }

    private fun generateIv(): ByteArray
    {
        val secureRandom = SecureRandom()
        val result = ByteArray(128 / 8)

        secureRandom.nextBytes(result)

        return result
    }
}