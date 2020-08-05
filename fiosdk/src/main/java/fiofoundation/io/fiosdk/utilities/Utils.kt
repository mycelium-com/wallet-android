package fiofoundation.io.fiosdk.utilities

import com.google.gson.GsonBuilder
import com.google.gson.Gson
import fiofoundation.io.fiosdk.formatters.FIOFormatter
import java.io.*


class Utils {
    companion object Static
    {
        @Throws(IOException::class, ClassNotFoundException::class)
        fun <T : Serializable> clone(`object`: T): T {
            val byteArrayOutputStream = ByteArrayOutputStream()
            val objectOutputStream = ObjectOutputStream(byteArrayOutputStream)

            objectOutputStream.writeObject(`object`)  // Could clone only the Transaction (i.e. this.transaction)

            val byteArrayInputStream = ByteArrayInputStream(byteArrayOutputStream.toByteArray())
            val objectInputStream = ObjectInputStream(byteArrayInputStream)

            @Suppress("UNCHECKED_CAST")
            return objectInputStream.readObject() as T
        }

        fun getGson(datePattern: String): Gson {
            return GsonBuilder()
                .setDateFormat(datePattern)
                .disableHtmlEscaping()
                .create()
        }

        fun generateActor(actorPublicAddress: String): String
        {
            if(actorPublicAddress.isNotEmpty())
            {
                val pKeyNoPrefix = actorPublicAddress.trimStart({ char ->
                    FIOFormatter.PATTERN_STRING_FIO_PREFIX_EOS.toCharArray().contains(char)
                })

                val b58Decode = PrivateKeyUtils.base58Decode(pKeyNoPrefix)

                return shortenKeyToString(shortenKey(b58Decode))
            }

            return ""
        }

        private fun shortenKey(publicKeyBytes: ByteArray): Long
        {
            var result:Long = 0
            var i = 1
            var len = 0

            while(len<=12)
            {
                var trimmedChar:Long = publicKeyBytes[i].toLong() and(if(len == 12)  0x0f else 0x1f)
                if (trimmedChar == 0L) { i++; continue; }

                var shuffle:Int = if(len == 12) 0 else 5*(12-len)-1

                result = result or(trimmedChar.shl(shuffle))

                len++
                i++
            }

            return result
        }

        private fun shortenKeyToString(shortenedKey: Long): String
        {
            val characters = ".12345abcdefghijklmnopqrstuvwxyz".toCharArray()
            var tmp = shortenedKey
            var str = CharArray(13)

            for (i:Int in 0..12)
            {
                var c:Char = characters[(tmp and(if(i == 0) 0x0f else 0x1f)).toInt()]
                str[12-i] = c
                tmp = tmp.shr(if(i == 0)  4 else 5)
            }

            return str.joinToString("").dropLast(1)
        }


    }
}