package com.mycelium.wallet.activity.util

import com.mycelium.view.Denomination
import com.mycelium.wapi.wallet.fiat.coins.FiatType
import com.mycelium.wapi.wallet.btc.coins.BitcoinMain
import com.mycelium.wapi.wallet.btc.coins.BitcoinTest
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.coins.GenericAssetInfo
import java.text.DecimalFormat


@JvmOverloads
fun Value.toStringWithUnit(denomination: Denomination = Denomination.UNIT): String {
    CoinFormat.maximumFractionDigits = type.unitExponent
    return String.format("%s %s", toString(denomination), denomination.getUnicodeString(type.symbol))
}

@JvmOverloads
fun Value.toString(denomination: Denomination = Denomination.UNIT): String {
    CoinFormat.maximumFractionDigits = type.unitExponent
    var result = valueAsBigDecimal
    if (type !is FiatType && denomination != Denomination.UNIT) {
        result = result.movePointRight(denomination.base10)
    }
    return CoinFormat.format(result)
}

fun GenericAssetInfo.isBtc(): Boolean {
    return this == BitcoinMain.get() || this == BitcoinTest.get()
}


private object CoinFormat : DecimalFormat() {
    init {
        groupingSize = 3
        isGroupingUsed = true
        maximumFractionDigits = 8
        val symbols = decimalFormatSymbols
        symbols.decimalSeparator = '.'
        symbols.groupingSeparator = ' '
        decimalFormatSymbols = symbols
    }
}