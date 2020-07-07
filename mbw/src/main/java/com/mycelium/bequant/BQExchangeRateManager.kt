package com.mycelium.bequant

import android.app.Activity
import com.mycelium.bequant.remote.repositories.Api
import com.mycelium.bequant.remote.trading.model.Symbol
import com.mycelium.wallet.BuildConfig
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.WalletApplication
import com.mycelium.wallet.exchange.GetExchangeRate
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
        fun exchangeSourceChanged()
    }

    private val preference by lazy { WalletApplication.getInstance().getSharedPreferences(Constants.EXCHANGE_RATES, Activity.MODE_PRIVATE) }
    private var _latestRates = mutableMapOf<String, MutableMap<String, BQExchangeRate>?>()
    private var _latestRatesTime: Long = 0

    @Volatile
    private var _fetcher: Fetcher? = null

    private var _subscribers = mutableListOf<Observer>()

    @Synchronized
    fun subscribe(subscriber: Observer) {
        _subscribers.add(subscriber)
    }

    @Synchronized
    fun unsubscribe(subscriber: Observer) {
        _subscribers.remove(subscriber)
    }

    class BQExchangeRate(val from: String, val to: String, price: Double, val time: Date? = Date()) :
            ExchangeRate("bequant", time?.time ?: 0, price, to)

    private var symbols = arrayOf<Symbol>()

    fun findSymbol(a: String, b: String): Symbol? =
            symbols.find { (it.baseCurrency == a && it.quoteCurrency == b) || (it.baseCurrency == b && it.quoteCurrency == a) }

    fun findSymbol(id: String): Symbol? = symbols.find { it.id == id }

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
                synchronized(_requestLock) {
                    setLatestRates(response)
                    _fetcher = null
                    notifyRefreshingExchangeRatesSucceeded()
                }

            }, { _, _ ->
                // we failed to get the exchange rate, try to restore saved values from the local database
                val savedExchangeRates = localValues()
                if (savedExchangeRates.isNotEmpty()) {
                    synchronized(_requestLock) {
                        setLatestRates(savedExchangeRates)
                        _fetcher = null
                        notifyRefreshingExchangeRatesSucceeded()
                    }
                } else {
                    synchronized(_requestLock) {
                        _fetcher = null
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
        _subscribers.forEach { it.refreshingExchangeRatesSucceeded() }
    }

    private fun notifyRefreshingExchangeRatesFailed() {
        _subscribers.forEach { it.refreshingExchangeRatesFailed() }
    }

    private fun notifyExchangeSourceChanged() {
        _subscribers.forEach { it.exchangeSourceChanged() }
    }

    // only refresh if last refresh is old
    fun requestOptionalRefresh() {
        if (System.currentTimeMillis() - _latestRatesTime > MIN_RATE_AGE_MS) {
            requestRefresh()
        }
    }

    fun requestRefresh() {
        synchronized(_requestLock) {
            // Only start fetching if we are not already on it
            if (_fetcher == null) {
                _fetcher = Fetcher()
                val t = Thread(_fetcher)
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
        _latestRates.clear()
        for (response in latestRates) {
            val fromCurrency = addSymbolDecorations(response.from)
            val toCurrency = addSymbolDecorations(response.to)
            if (_latestRates[fromCurrency] != null) {
                _latestRates[fromCurrency]?.set(toCurrency, response)
            } else {
                _latestRates[fromCurrency] = mutableMapOf(toCurrency to response)
            }
            storeExchangeRate(fromCurrency, toCurrency, response.price)
        }
        _latestRatesTime = System.currentTimeMillis()
    }

    private fun storeExchangeRate(fromCurrency: String, toCurrency: String, price: Double?) {
        preference.edit().putString(fromCurrency + "_" + toCurrency, price?.toString()).apply()
    }

    /**
     * Get the exchange rate for the specified currency.
     *
     *
     * Returns null if the current rate is too old
     * In that the case the caller could choose to call refreshRates() and listen
     * for callbacks. If a rate is returned the contained price may be null if
     * the currently chosen exchange source is not available.
     */

    fun getExchangeRate(source: String, destination: String, exchangeSource: String): ExchangeRate? {
        val latestRatesForSourceCurrency: Map<String, BQExchangeRate>? = _latestRates[source]

        if (latestRatesForSourceCurrency == null || latestRatesForSourceCurrency.isEmpty() || !latestRatesForSourceCurrency.containsKey(destination)) {
            return null
        }
        val latestRatesForTargetCurrency = latestRatesForSourceCurrency[destination]
                ?: //rate is too old or does not exists, exchange source seems to not be available
                //we return a rate with null price to indicate there is something wrong with the exchange rate source
                return ExchangeRate.missingRate(exchangeSource, System.currentTimeMillis(), destination)
        val exchangeRate = latestRatesForTargetCurrency

        if (exchangeRate.name == exchangeSource) {
            //if the price is 0, obviously something went wrong
            return if (exchangeRate.price == 0.0) {
                //we return an exchange rate with null price -> indicating missing rate
                ExchangeRate.missingRate(exchangeSource, System.currentTimeMillis(), destination)
            } else exchangeRate
            //everything is fine, return the rate
        }
        return null
    }

    override fun getExchangeRate(fromCurrency: String, toCurrency: String): ExchangeRate? =
            getExchangeRate(fromCurrency, toCurrency, "bequant")


    fun get(value: Value, toCurrency: GenericAssetInfo): Value? {
        val rate = GetExchangeRate(MbwManager.getInstance(WalletApplication.getInstance()).getWalletManager(false),
                toCurrency.symbol, value.type.symbol, this).apply { hack = true }.invoke()
        val rateValue = rate.rate
        return if (rateValue != null) {
            val bigDecimal = rateValue.multiply(BigDecimal(value.value))
                    .movePointLeft(value.type.unitExponent)
                    .round(MathContext.DECIMAL128)
            parse(toCurrency, bigDecimal)
        } else {
            null
        }
    }


    private val MIN_RATE_AGE_MS = TimeUnit.SECONDS.toMillis(5)
    val BTC = "BTC"
    private val _requestLock = Any()

    /**
     * the method is used to remove additional characters indicating testnet coins from currencies' symbols
     * before making request to the server with these symbols as parameters, as server provides
     * exchange rates only by pure symbols, i.e. BTC and not tBTC
     */
    private fun trimSymbolDecorations(symbol: String): String? {
        if (BuildConfig.FLAVOR == "btctestnet") {
            if (symbol.startsWith("t")) {
                return symbol.substring(1)
            }
            if (symbol.endsWith("t")) {
                return symbol.substring(0, symbol.length - 1)
            }
        }
        return symbol
    }

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