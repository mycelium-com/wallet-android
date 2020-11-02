package com.mycelium.bequant.common

import com.mycelium.bequant.remote.trading.model.Currency
import com.mycelium.wallet.Utils
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.GenericAssetInfo
import com.mycelium.wapi.wallet.fiat.coins.FiatType
import java.util.*


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
            else -> CryptoCurrency(this@assetInfoById.id, this@assetInfoById.fullName, this@assetInfoById.id, this@assetInfoById.precisionPayout, 2, true)
        }
    } else {
        FiatType(id.substring(0, 3))
    }
}
