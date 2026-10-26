package com.mycelium.wallet.activity.util

import com.mycelium.view.Denomination
import com.mycelium.wapi.wallet.Util
import com.mycelium.wapi.wallet.btc.coins.BitcoinMain
import com.mycelium.wapi.wallet.btc.coins.BitcoinTest
import com.mycelium.wapi.wallet.coins.AssetInfo
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.fiat.coins.FiatType
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat


@JvmOverloads
fun Value.toStringWithUnit(denomination: Denomination = Denomination.UNIT): String =
    format(denomination, type.unitExponent, withUnit = true)

@JvmOverloads
fun Value.toStringFriendlyWithUnit(denomination: Denomination = Denomination.UNIT): String =
    format(denomination, type.friendlyDigits, withUnit = true)

@JvmOverloads
fun Value.toFriendlyString(denomination: Denomination = Denomination.UNIT): String =
    format(denomination, type.friendlyDigits, withUnit = false)

@JvmOverloads
fun Value.toString(denomination: Denomination = Denomination.UNIT): String =
    format(denomination, type.unitExponent, withUnit = false)

/**
 * Same as [toString] but capped at [maxFractionDigits], rounding rather than
 * cutting the string short. Used where a compact number is wanted — an ETH
 * balance carries 18 decimals, and showing every one of them is unreadable.
 */
fun Value.toString(denomination: Denomination, maxFractionDigits: Int): String =
    format(denomination, maxFractionDigits, withUnit = false)

private fun Value.format(denomination: Denomination, maxFractionDigits: Int, withUnit: Boolean): String {
    var result: BigDecimal = valueAsBigDecimal
    val isFiat = type is FiatType
    if (!isFiat && denomination != Denomination.UNIT) {
        result = result.movePointRight(denomination.scale)
    }
    val formatter = coinFormat()
    formatter.maximumFractionDigits = maxFractionDigits
    formatter.minimumFractionDigits = if (isFiat) 2 else 0
    val number = formatter.format(result)
    if (!withUnit) return number
    val unit = if (isFiat) type.symbol else denomination.getUnicodeString(type.displaySymbol)
    return String.format("%s %s", number, unit)
}

/** [AssetInfo.getSymbol] as shown to the user, see [Util.displaySymbol]. */
val AssetInfo.displaySymbol: String
    get() = Util.displaySymbol(symbol)

fun AssetInfo.isBtc(): Boolean {
    return this == BitcoinMain || this == BitcoinTest
}

/**
 * One formatter per thread. [DecimalFormat] is not thread-safe and every helper
 * above mutates the digit settings right before formatting, so a single shared
 * instance let a background balance update and the UI thread interleave a
 * settings change with someone else's format() call — and left whatever digit
 * count the previous caller wanted in place for the next one.
 */
private val COIN_FORMAT = object : ThreadLocal<DecimalFormat>() {
    override fun initialValue(): DecimalFormat = DecimalFormat().apply {
        groupingSize = 3
        isGroupingUsed = true
        maximumFractionDigits = 8
        roundingMode = RoundingMode.HALF_UP
        decimalFormatSymbols = decimalFormatSymbols.apply {
            decimalSeparator = '.'
            groupingSeparator = ' '
        }
    }
}

private fun coinFormat(): DecimalFormat = COIN_FORMAT.get()!!
