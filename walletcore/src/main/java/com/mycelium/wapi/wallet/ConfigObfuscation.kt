package com.mycelium.wapi.wallet

import com.mrd.bitlib.util.HexUtils
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Obfuscation of values that ship inside the app anyway (third-party API keys in the bundled or
 * remote configuration), compatible with the WalletKMP `AesKeyCipher.obfuscationCipher` format:
 *
 *  - `enc1_<hex>` — AES-128-CTR, `[IV 16 bytes][ciphertext]`, plaintext is raw bytes → returned as hex
 *  - `enc2_<hex>` — same, plaintext is UTF-8 text → returned as the string
 *  - anything else is returned unchanged
 *
 * The key is a constant compiled into the binary, so this hides values from casual grepping of the
 * config files only — it is not protection. Nothing security-relevant may rely on it.
 */
object ConfigObfuscation {
    private const val PREFIX_BYTES = "enc1_"
    private const val PREFIX_TEXT = "enc2_"
    private const val IV_LENGTH = 16
    private val KEY = HexUtils.toBytes("a8105c3c8b75556a9099b8dcab9cc133")

    /** Decodes a config value; unknown/undecodable values are returned as-is (never throws). */
    @JvmStatic
    fun decode(value: String?): String? {
        if (value == null) return null
        return try {
            when {
                value.startsWith(PREFIX_TEXT) -> String(decrypt(HexUtils.toBytes(value.substring(PREFIX_TEXT.length))), Charsets.UTF_8)
                value.startsWith(PREFIX_BYTES) -> HexUtils.toHex(decrypt(HexUtils.toBytes(value.substring(PREFIX_BYTES.length))))
                else -> value
            }
        } catch (e: Exception) {
            value
        }
    }

    /** `enc2_` encoding of a text value (used to prepare config files, e.g. from a unit test). */
    @JvmStatic
    fun encodeText(plain: String, iv: ByteArray = randomIv()): String =
        PREFIX_TEXT + HexUtils.toHex(encrypt(plain.toByteArray(Charsets.UTF_8), iv))

    /** `enc1_` encoding of raw bytes. */
    @JvmStatic
    fun encodeBytes(plain: ByteArray, iv: ByteArray = randomIv()): String =
        PREFIX_BYTES + HexUtils.toHex(encrypt(plain, iv))

    private fun decrypt(data: ByteArray): ByteArray {
        require(data.size >= IV_LENGTH) { "obfuscated value too short" }
        val iv = data.copyOfRange(0, IV_LENGTH)
        val ct = data.copyOfRange(IV_LENGTH, data.size)
        return ctr(Cipher.DECRYPT_MODE, iv).doFinal(ct)
    }

    private fun encrypt(plain: ByteArray, iv: ByteArray): ByteArray {
        require(iv.size == IV_LENGTH)
        return iv + ctr(Cipher.ENCRYPT_MODE, iv).doFinal(plain)
    }

    private fun ctr(mode: Int, iv: ByteArray): Cipher =
        Cipher.getInstance("AES/CTR/NoPadding").apply {
            init(mode, SecretKeySpec(KEY, "AES"), IvParameterSpec(iv))
        }

    private fun randomIv() = ByteArray(IV_LENGTH).also { SecureRandom().nextBytes(it) }
}
