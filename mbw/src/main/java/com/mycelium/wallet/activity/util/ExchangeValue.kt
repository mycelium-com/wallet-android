package com.mycelium.wallet.activity.util

import com.mycelium.wallet.ExchangeRateManager
import com.mycelium.wallet.exchange.GetExchangeRate
import com.mycelium.wapi.wallet.coins.GenericAssetInfo
import com.mycelium.wapi.wallet.coins.Value
import java.math.BigDecimal
import java.math.MathContext

/**
 * this need for get amount activity for case not correct exchange with small value(BTC->USD->BTC)
 */
class ExchangeValue(value: Value, val baseValue: Value) : Value(value.type, value.value) {
    override fun isZero(): Boolean {
        return baseValue.isZero
    }
}

fun ExchangeRateManager.get(value: Value, toCurrency: GenericAssetInfo): Value? {
    if(toCurrency == value.type) {
        return value
    }
    var fromValue = value
    if (value is ExchangeValue) {
        fromValue = value.baseValue
    }
    val rate = GetExchangeRate(toCurrency.symbol, fromValue.type.symbol, this).invoke()
    val rateValue = rate.rate
    if (rateValue != null) {
        val bigDecimal = rateValue.multiply(BigDecimal.valueOf(fromValue.value))
                .movePointLeft(fromValue.type.unitExponent)
                .round(MathContext.DECIMAL32)
        return ExchangeValue(Value.parse(toCurrency, bigDecimal), fromValue)
    } else {
        return null
    }
}