package fiofoundation.io.fiosdk

import java.lang.Exception
import fiofoundation.io.fiosdk.utilities.SUFUtils
import java.math.BigInteger

fun ByteArray.toHexString():String
{
    val HEX_CHARS = "0123456789ABCDEF".toCharArray()

    val result = StringBuffer()

    forEach {
        val octet = it.toInt()
        val firstIndex = (octet and 0xF0).ushr(4)
        val secondIndex = octet and 0x0F
        result.append(HEX_CHARS[firstIndex])
        result.append(HEX_CHARS[secondIndex])
    }

    return result.toString()
}

fun String.hexStringToByteArray(returnUnsignedIntegers:Boolean=false) : ByteArray
{
    val HEX_CHARS = "0123456789ABCDEF".toCharArray()

    val result = ByteArray(length / 2)

    for (i in 0 until length step 2) {
        val firstIndex = HEX_CHARS.indexOf(this[i])
        val secondIndex = HEX_CHARS.indexOf(this[i + 1])

        val octet = firstIndex.shl(4).or(secondIndex)
        result.set(i.shr(1), octet.toByte())
    }

    if(returnUnsignedIntegers)
        return result.filter { byte->byte.compareTo(-1)!=0 }.toByteArray()
    else
        return result
}

fun String.isFioAddress(): Boolean
{
    if(this.isNotEmpty())
    {
        if(this.length in 3..64)
        {
            val fioRegEx = Regex("^(?:(?=.{3,64}\$)[a-zA-Z0-9]{1}(?:(?!-{2,}))[a-zA-Z0-9-]*(?:(?<!-))@[a-zA-Z0-9]{1}(?:(?!-{2,}))[a-zA-Z0-9-]*(?:(?<!-))\$)",RegexOption.IGNORE_CASE)
            if(fioRegEx.matchEntire(this)!=null)
                return true
        }
    }

    return false
}

fun String.isFioDomain(): Boolean
{
    if(this.isNotEmpty())
    {
        if(this.length in 1..62)
        {
            val fioRegEx = Regex("^[a-zA-Z0-9\\\\-]+\$")
            if(fioRegEx.matchEntire(this)!=null)
                return true
        }
    }

    return false
}

fun String.isTokenCode(): Boolean
{
    if(this.isNotEmpty())
    {
        if(this.length in 1..10)
        {
            val fioRegEx = Regex("^[a-zA-Z0-9]+\$")
            if(fioRegEx.matchEntire(this)!=null)
                return true
        }
    }

    return false
}

fun String.isChainCode(): Boolean
{
    if(this.isNotEmpty())
    {
        if(this.length in 1..10)
        {
            val fioRegEx = Regex("^[a-zA-Z0-9]+\$")
            if(fioRegEx.matchEntire(this)!=null)
                return true
        }
    }

    return false
}

fun String.isFioPublicKey(): Boolean
{
    if(this.isNotEmpty())
    {
        val fioRegEx = Regex("^FIO.+\$")
        if(fioRegEx.matchEntire(this)!=null)
            return true
    }

    return false
}

fun String.isNativeBlockChainPublicAddress(): Boolean
{
    if(this.isNotEmpty())
    {
        if(this.length in 1..128) {
            return true
        }
    }

    return false
}

fun String.toMultiLevelAddress(): MutableMap<String,String>
{
    var mla = mutableMapOf<String,String>()

    if(!this.contains("?"))
    {
        mla.put("address",this)
        return mla
    }
    else
    {
        try {
            val mlaItems = this.split("?")
            if(mlaItems.count()>0) mla.put("address",mlaItems[0]) else mla.put("address",this)

            val attributes = if(mlaItems.count()>1) mlaItems[1].split("&") else null

            if(!attributes.isNullOrEmpty())
            {
                attributes.forEach{
                    val attItems = it.split("=")

                    if(attItems.count()>1)
                        mla.put(attItems[0],attItems[1])
                }
            }

            return mla
        }
        catch (e:Exception)
        {
            mla.put("address",this)
            return mla
        }

    }

}

fun String.toFIO(): Double
{
    return SUFUtils.fromSUFtoAmount(this)
}

fun String.toSUF(): BigInteger
{
    try {
        return SUFUtils.amountToSUF(this.toBigDecimal().toDouble())
    }
    catch(e:Error)
    {
        return BigInteger.ZERO
    }
}

fun Double.toSUF(): BigInteger
{
    return SUFUtils.amountToSUF(this)
}

fun BigInteger.toFIO(): Double
{
    return SUFUtils.fromSUFtoAmount(this)
}
