package fiofoundation.io.fiosdk.utilities

import java.security.MessageDigest

object HashUtils {
    fun sha512(input: String) = hashString("SHA-512", input)
    fun sha512(input: ByteArray) = hashBytes("SHA-512", input)

    fun sha256(input: String) = hashString("SHA-256", input)

    fun sha1(input: String) = hashString("SHA-1", input)

    private fun hashString(type: String, input: String): String {
        val HEX_CHARS = "0123456789ABCDEFabcdef"
        val bytes = MessageDigest
            .getInstance(type)
            .digest(input.toByteArray())


        val result = StringBuilder(bytes.size * 2)

        bytes.forEach {
            val i = it.toInt()
            result.append(HEX_CHARS[i shr 4 and 0x0f])
            result.append(HEX_CHARS[i and 0x0f])
        }

        return result.toString()
    }

    private fun hashBytes(type: String, input: ByteArray): ByteArray {
        val bytes = MessageDigest
            .getInstance(type)
            .digest(input)

        return bytes
    }
}