package com.mycelium.wallet.activity.util

import com.mycelium.view.Denomination
import com.mycelium.wapi.wallet.btc.coins.BitcoinMain
import com.mycelium.wapi.wallet.btc.coins.BitcoinTest
import com.mycelium.wapi.wallet.coins.AssetInfo
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.fiat.coins.FiatType
import java.text.DecimalFormat


@JvmOverloads
fun Value.toStringWithUnit(denomination: Denomination = Denomination.UNIT): String {
    CoinFormat.maximumFractionDigits = type.unitExponent
    var value = type.symbol
    if (type !is FiatType) {
        value = denomination.getUnicodeString(type.symbol)
    }
    return String.format("%s %s", toString(denomination), value)
}

@JvmOverloads
fun Value.toStringFriendlyWithUnit(denomination: Denomination = Denomination.UNIT): String {
    CoinFormat.maximumFractionDigits = type.friendlyDigits
    var value = type.symbol
    if (type !is FiatType) {
        value = denomination.getUnicodeString(type.symbol)
    }
    return String.format("%s %s", toFriendlyString(denomination), value)
}

@JvmOverloads
fun Value.toFriendlyString(denomination: Denomination = Denomination.UNIT): String {
    CoinFormat.maximumFractionDigits = type.friendlyDigits
    CoinFormat.minimumFractionDigits = 0
    var result = valueAsBigDecimal
    if (type !is FiatType && denomination != Denomination.UNIT) {
        result = result.movePointRight(denomination.scale)
    } else if (type is FiatType) {
        CoinFormat.minimumFractionDigits = 2
    }
    return CoinFormat.format(result)
}

@JvmOverloads
fun Value.toString(denomination: Denomination = Denomination.UNIT): String {
    CoinFormat.maximumFractionDigits = type.unitExponent
    CoinFormat.minimumFractionDigits = 0
    var result = valueAsBigDecimal
    if (type !is FiatType && denomination != Denomination.UNIT) {
        result = result.movePointRight(denomination.scale)
    } else if (type is FiatType) {
        CoinFormat.minimumFractionDigits = 2
    }
    return CoinFormat.format(result)
}

fun AssetInfo.isBtc(): Boolean {
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