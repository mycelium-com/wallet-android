package fiofoundation.io.fiosdk.formatters

import com.google.common.base.CharMatcher
import com.google.common.base.Strings
import org.bitcoinj.core.Sha256Hash
import org.bouncycastle.util.encoders.Base64
import org.bouncycastle.util.encoders.Hex

class ByteFormatter(private val context: ByteArray) {

    companion object {

        private const val BASE64_PADDING = 4
        private const val BASE64_PADDING_CHAR = '='

        fun createFromBase64(base64String: String): ByteFormatter {
            val trimmed = CharMatcher.`is`(BASE64_PADDING_CHAR).removeFrom(base64String)
            val padded = Strings.padEnd(trimmed,(trimmed.length + BASE64_PADDING - 1) / BASE64_PADDING * BASE64_PADDING, BASE64_PADDING_CHAR)
            return ByteFormatter(Base64.decode(padded))
        }

        fun createFromHex(hexString: String): ByteFormatter {
            val data = Hex.decode(hexString)
            return ByteFormatter(data)
        }
    }

    fun sha256(): ByteFormatter {
        return ByteFormatter(Sha256Hash.hash(this.context))
    }

    fun toHex(): String {
        return Hex.toHexString(this.context)
    }
}