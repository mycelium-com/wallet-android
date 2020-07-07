package com.mycelium.bequant.common

import com.mycelium.bequant.remote.trading.model.Currency
import com.mycelium.wallet.Utils
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.GenericAssetInfo
import com.mycelium.wapi.wallet.fiat.coins.FiatType


fun String.passwordLevel(): Int {
    var score = 0
    var upper = false
    var lower = false
    var digit = false
    var specialChar = false
    if (length in 1..5) {
        return 1
    }
    for (i in indices) {
        val c: Char = this[i]
        if (!specialChar && !Character.isLetterOrDigit(c)) {
            score++
            specialChar = true
        } else if (!digit && Character.isDigit(c)) {
            score++
            digit = true
        } else if (!upper && Character.isUpperCase(c)) {
            score++
            upper = true
        } else if (!lower && Character.isLowerCase(c)) {
            score++
            lower = true
        }
    }
    return score
}

fun <T> equalsValuesBy(a: T, b: T, vararg selectors: (T) -> Any?): Boolean {
    require(selectors.isNotEmpty())
    return equalsValuesByImpl(a, b, selectors)
}

private fun <T> equalsValuesByImpl(a: T, b: T, selectors: Array<out (T) -> Any?>): Boolean {
    for (fn in selectors) {
        if (fn(a) != fn(b)) return false
    }
    return true
}

fun Currency.assetInfoById(): GenericAssetInfo {
    return if (crypto) {
        when (id) {
            "BTC" -> Utils.getBtcCoinType()
            "ETH" -> Utils.getEthCoinType()
            else -> CryptoCurrency( this@assetInfoById.id, this@assetInfoById.fullName, this@assetInfoById.id, this@assetInfoById.precisionPayout, 2, true)
        }
    } else {
        FiatType(id.substring(0, 3))
    }
}