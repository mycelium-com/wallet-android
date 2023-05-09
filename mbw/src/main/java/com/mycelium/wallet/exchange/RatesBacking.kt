package com.mycelium.wallet.exchange

import com.mycelium.generated.rates.database.RatesDB
import com.mycelium.wapi.model.ExchangeRate


class RatesBacking(val database: RatesDB) {
    private val queries = database.ratesQueries

    fun allExchangeRates(): List<ExchangeRate> =
            queries.selectAll(mapper = { from, to, market, rate, time ->
                ExchangeRate(market, time, rate, from, to)
            }).executeAsList()

    fun storeExchangeRates(fromCurrency: String, rates: List<ExchangeRate>) {
        queries.transaction {
            rates.forEach {
                queries.insertRate(fromCurrency, it.currency, it.name, it.price, it.time)
            }
        }
    }
}