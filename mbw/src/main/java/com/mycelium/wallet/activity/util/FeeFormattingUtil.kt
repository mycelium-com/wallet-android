package com.mycelium.wallet.activity.util

import com.mycelium.view.Denomination
import com.mycelium.wapi.wallet.btc.coins.BitcoinMain
import com.mycelium.wapi.wallet.btc.coins.BitcoinTest
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.eth.coins.EthMain
import com.mycelium.wapi.wallet.eth.coins.EthTest
import org.web3j.utils.Convert
import java.math.RoundingMode
import kotlin.math.log10
import kotlin.math.roundToLong

class FeeFormattingUtil {
    fun getFeeFormatter(coinType: CryptoCurrency): FeeFormatter? {
        return when (coinType) {
            is BitcoinMain, is BitcoinTest -> BtcFeeFormatter()
            is EthMain, EthTest -> EthFeeFormatter()
            else -> null
        }
    }
}

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
    // value comes here in per kbyte, i.e. divided by 1000
    override fun getFeeAbsValue(value: Value) = (value * 1000).toStringWithUnit()

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