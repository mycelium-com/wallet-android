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
}

class EthFeeFormatter : FeeFormatter {
    override fun getFeeAbsValue(value: Value) = value.toStringWithUnit(Denomination.MILLI)

    override fun getAltValue(value: Value) = "~${value.toStringWithUnit()}"

    override fun getFeePerUnit(value: Long) = "${Convert.fromWei(value.toBigDecimal(),
            Convert.Unit.GWEI).setScale(2, RoundingMode.HALF_UP)} Gwei/gas"
}