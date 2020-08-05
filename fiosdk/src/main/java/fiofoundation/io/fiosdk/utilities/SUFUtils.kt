package fiofoundation.io.fiosdk.utilities

import fiofoundation.io.fiosdk.models.Constants
import java.math.BigInteger

object SUFUtils {

    fun amountToSUF(amount:Double):BigInteger
    {
        return (Constants.SUFUnit.toDouble() * amount).toBigDecimal().toBigInteger()
    }

    fun fromSUFtoAmount(suf:BigInteger):Double
    {
        return suf.toBigDecimal().toDouble() / Constants.SUFUnit.toDouble()
    }

    fun fromSUFtoAmount(suf:String):Double
    {
        try {
            return suf.toBigDecimal().toDouble() / Constants.SUFUnit.toDouble()
        }
        catch(e:Error)
        {
            return 0.0
        }
    }
}