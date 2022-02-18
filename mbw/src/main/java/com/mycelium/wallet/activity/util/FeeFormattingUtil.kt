package com.mycelium.wallet.activity.util

import com.mycelium.view.Denomination
import com.mycelium.wapi.wallet.coins.Value
import org.web3j.utils.Convert
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToLong

interface FeeFormatter {
    fun getFeeAbsValue(value: Value): String
    fun getAltValue(value: Value): String
    fun getFeePerUnit(value: Long): String
}

class BtcFeeFormatter : FeeFormatter {
    override fun getFeeAbsValue(value: Value) = value.toStringWithUnit(Denomination.MILLI)

    override fun getAltValue(value: Value) = "~${value.toStringWithUnit()}"

    override fun getFeePerUnit(value: Long) = "${(value / 1000f).roundToLong()} sat/byte"

    fun getFeePerUnitInBytes(value: Long) = "$value sat/byte"
}

class EthFeeFormatter : FeeFormatter {
    override fun getFeeAbsValue(value: Value): String {
        // value with more than 8 decimal digits converted to string with unit later doesn't fit the view and is cut poorly
        val rounded = value.valueAsBigDecimal.setScale(8, BigDecimal.ROUND_HALF_EVEN)
        val roundedValue = Value.valueOf(
            value.type,
            (rounded * 10.0.pow(value.type.unitExponent).toBigDecimal()).toBigInteger()
        )
        return roundedValue.toStringWithUnit(Denomination.UNIT)
    }

    override fun getAltValue(value: Value) = if (value.isZero()) {
        "<" + (value + 1).toStringWithUnit()
    } else {
        "~" + value.toStringWithUnit()
    }

    override fun getFeePerUnit(value: Long): String {
        val length = (log10(value.toDouble()) + 1).toInt()
        val format = getFormat(length)
        return "${Convert.fromWei(value.toBigDecimal(), format).setScale(2, RoundingMode.HALF_UP)} " +
                "${format.toString().capitalize()}/gas"
    }

    private fun getFormat(value: Int): Convert.Unit {
        return when (value) {
            in 0..3 -> Convert.Unit.WEI
            in 3..6 -> Convert.Unit.KWEI
            in 6..9 -> Convert.Unit.MWEI
            in 9..12 -> Convert.Unit.GWEI
            in 12..15 -> Convert.Unit.SZABO
            in 15..18 -> Convert.Unit.FINNEY
            in 18..21 -> Convert.Unit.ETHER
            in 21..24 -> Convert.Unit.KETHER
            in 24..27 -> Convert.Unit.METHER
            else -> Convert.Unit.GETHER
        }
    }
}