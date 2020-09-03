package com.mycelium.bequant

import android.app.Activity
import com.mycelium.bequant.remote.repositories.Api
import com.mycelium.bequant.remote.trading.model.Symbol
import com.mycelium.wallet.BuildConfig
import com.mycelium.wallet.WalletApplication
import com.mycelium.wapi.model.ExchangeRate
import com.mycelium.wapi.wallet.coins.GenericAssetInfo
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.coins.Value.Companion.parse
import com.mycelium.wapi.wallet.currency.ExchangeRateProvider
import kotlinx.coroutines.GlobalScope
import java.math.BigDecimal
import java.math.MathContext
import java.util.*
import java.util.concurrent.TimeUnit


object BQExchangeRateManager : ExchangeRateProvider {
    interface Observer {
        fun refreshingExchangeRatesSucceeded()
        fun refreshingExchangeRatesFailed()
    }

    private val preference by lazy { WalletApplication.getInstance().getSharedPreferences(Constants.EXCHANGE_RATES, Activity.MODE_PRIVATE) }
    private var latestRates = mutableMapOf<String, MutableMap<String, BQExchangeRate>?>()
    private var latestRatesTime: Long = 0

    @Volatile
    private var fetcher: Fetcher? = null

    private var subscribers = mutableListOf<Observer>()

    @Synchronized
    fun subscribe(subscriber: Observer) {
        subscribers.add(subscriber)
    }

    @Synchronized
    fun unsubscribe(subscriber: Observer) {
        subscribers.remove(subscriber)
    }

    class BQExchangeRate(val from: String, val to: String, price: Double, val time: Date? = Date()) :
            ExchangeRate("bequant", time?.time ?: 0, price, to)

    private var symbols = arrayOf<Symbol>()

    fun findSymbol(youGetSymbol: String, youSendSymbol: String, answer: (Symbol?) -> Unit) {
        Api.publicRepository.publicSymbolGet(GlobalScope, null, { symbolArray ->
            if (symbolArray?.isNotEmpty() == true) {
                symbols = symbolArray
                answer(symbols.find {
                    it.baseCurrency == youGetSymbol && it.quoteCurrency == youSendSymbol
                            || it.baseCurrency == youSendSymbol && it.quoteCurrency == youGetSymbol
                })
            }
        }, { _, _ -> })
    }

    private class Fetcher : Runnable {
        override fun run() {
            if (symbols.isEmpty()) {
                Api.publicRepository.publicSymbolGet(GlobalScope, null, {
                    if (it?.isNotEmpty() == true) {
                        symbols = it
                        getRates()
                    } else {
                        notifyRefreshingExchangeRatesFailed()
                    }
                }, { _, _ -> notifyRefreshingExchangeRatesFailed() }, {})
            } else {
                getRates()
            }
        }

        fun getRates() {
            Api.publicRepository.publicTickerGet(GlobalScope, null, { tikers ->
                val response = mutableListOf<BQExchangeRate>()
                tikers?.forEach { ticker ->
                    symbols.find { it.id == ticker.symbol }?.let { symbol ->
                        response.add(BQExchangeRate(symbol.baseCurrency, symbol.quoteCurrency,
                                ticker.last ?: 0.0, ticker.timestamp))
                    }
                }
                synchronized(requestLock) {
                    setLatestRates(response)
                    fetcher = null
                    notifyRefreshingExchangeRatesSucceeded()
                }

            }, { _, _ ->
                // we failed to get the exchange rate, try to restore saved values from the local database
                val savedExchangeRates = localValues()
                if (savedExchangeRates.isNotEmpty()) {
                    synchronized(requestLock) {
                        setLatestRates(savedExchangeRates)
                        fetcher = null
                        notifyRefreshingExchangeRatesSucceeded()
                    }
                } else {
                    synchronized(requestLock) {
                        fetcher = null
                        notifyRefreshingExchangeRatesFailed()
                    }
                }
            }, {})
        }
    }


    private fun localValues(): List<BQExchangeRate> =
            mutableListOf<BQExchangeRate>().apply {
                preference.all.forEach {
                    val currencies = it.key.split("_")
                    val fromCurrency = currencies[0]
                    val toCurrency = currencies[1]
                    val price = try {
                        preference.getString(it.key, "0")?.toDouble() ?: 0.0
                    } catch (nfe: NumberFormatException) {
                        0.0
                    }
                    add(BQExchangeRate(fromCurrency, toCurrency, price))
                }
            }

    private fun notifyRefreshingExchangeRatesSucceeded() {
        subscribers.forEach { it.refreshingExchangeRatesSucceeded() }
    }

    private fun notifyRefreshingExchangeRatesFailed() {
        subscribers.forEach { it.refreshingExchangeRatesFailed() }
    }

    // only refresh if last refresh is old
    fun requestOptionalRefresh() {
        if (System.currentTimeMillis() - latestRatesTime > MIN_RATE_AGE_MS) {
            requestRefresh()
        }
    }

    private fun requestRefresh() {
        synchronized(requestLock) {
            // Only start fetching if we are not already on it
            if (fetcher == null) {
                fetcher = Fetcher()
                val t = Thread(fetcher)
                t.isDaemon = true
                t.start()
            }
        }
    }

    @Synchronized
    private fun setLatestRates(latestRates: List<BQExchangeRate>) {
        if (latestRates.isEmpty()) {
            return
        }
        this.latestRates.clear()
        for (response in latestRates) {
            val fromCurrency = addSymbolDecorations(response.from)
            val toCurrency = addSymbolDecorations(response.to)
            if (this.latestRates[fromCurrency] != null) {
                this.latestRates[fromCurrency]?.set(toCurrency, response)
            } else {
                this.latestRates[fromCurrency] = mutableMapOf(toCurrency to response)
            }
            storeExchangeRate(fromCurrency, toCurrency, response.price)
        }
        latestRatesTime = System.currentTimeMillis()
    }

    private fun storeExchangeRate(fromCurrency: String, toCurrency: String, price: Double?) {
        preference.edit().putString(fromCurrency + "_" + toCurrency, price?.toString()).apply()
    }

    /**
     * Get the exchange rate for the specified currency.
     *
     * Returns null if the current rate is too old
     * In that the case the caller could choose to call refreshRates() and listen
     * for callbacks. If a rate is returned the contained price may be null if
     * the currently chosen exchange source is not available.
     */
    override fun getExchangeRate(source: String, destination: String): ExchangeRate? {
        val exchangeSource = "bequant"
        val latestRatesForSourceCurrency: Map<String, BQExchangeRate>? = latestRates[source]

        if (latestRatesForSourceCurrency?.containsKey(destination) != true) {
            return null
        }
        val latestRatesForTargetCurrency = latestRatesForSourceCurrency[destination]
                ?: //rate is too old or does not exists, exchange source seems to not be available
                //we return a rate with null price to indicate there is something wrong with the exchange rate source
                return ExchangeRate.missingRate(exchangeSource, System.currentTimeMillis(), destination)

        if (latestRatesForTargetCurrency.name == exchangeSource) {
            //if the price is 0, obviously something went wrong
            return if (latestRatesForTargetCurrency.price == 0.0) {
                //we return an exchange rate with null price -> indicating missing rate
                ExchangeRate.missingRate(exchangeSource, System.currentTimeMillis(), destination)
            } else latestRatesForTargetCurrency
            //everything is fine, return the rate
        }
        return null
    }

    fun get(value: Value, toCurrency: GenericAssetInfo): Value? {
        val price = getExchangeRate(value.type.symbol, toCurrency.symbol)?.let {
            BigDecimal.valueOf(it.price)
        } ?: getExchangeRate(toCurrency.symbol, value.type.symbol)?.let {
            BigDecimal.valueOf(1 / it.price)
        }
        return price
                ?.multiply(BigDecimal(value.value))
                ?.movePointLeft(value.type.unitExponent)
                ?.round(MathContext.DECIMAL128)
                ?.let {
                    parse(toCurrency, it)
                }
    }

    private val MIN_RATE_AGE_MS = TimeUnit.SECONDS.toMillis(5)
    const val BTC = "BTC"
    private val requestLock = Any()

    private fun addSymbolDecorations(symbol: String): String {
        if (BuildConfig.FLAVOR == "btctestnet") {
            if (symbol == "BTC") {
                return "t$symbol"
            }
            if (symbol == "MT") {
                return symbol + "t"
            }
        }
        return symbol
    }
}