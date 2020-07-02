package com.mycelium.bequant.remote.repositories

import com.mycelium.bequant.remote.doRequest
import com.mycelium.bequant.remote.trading.api.PublicApi
import com.mycelium.bequant.remote.trading.model.*
import kotlinx.coroutines.CoroutineScope

class PublicApiRespository {

    private val api = PublicApi.create()


    fun publicCandlesGet(scope: CoroutineScope,
                         symbols: String, period: String, sort: String, from: String, till: String, limit: Int, offset: Int,
                         success: (Any?) -> Unit, error: (Int, String) -> Unit, finally: () -> Unit) {
        doRequest(scope, {
            api.publicCandlesGet(symbols, period, sort, from, till, limit, offset)
        }, successBlock = success, errorBlock = error, finallyBlock = finally)
    }


    fun publicCandlesSymbolGet(scope: CoroutineScope, symbol: String, period: String, sort: String, from: String, till: String, limit: Int, offset: Int,
                               success: (Array<Candle>?) -> Unit, error: (Int, String) -> Unit, finally: () -> Unit) {
        doRequest(scope, {
            api.publicCandlesSymbolGet(symbol, period, sort, from, till, limit, offset)
        }, successBlock = success, errorBlock = error, finallyBlock = finally)
    }

    fun publicCurrencyCurrencyGet(scope: CoroutineScope, currency: String,
                                  success: (Currency?) -> Unit, error: (Int, String) -> Unit, finally: () -> Unit) {
        doRequest(scope, {
            api.publicCurrencyCurrencyGet(currency)
        }, successBlock = success, errorBlock = error, finallyBlock = finally)
    }

    // maybe need put it to sharedpreference
    private var publicCurrencies = arrayOf<Currency>()

    fun publicCurrencyGet(scope: CoroutineScope, currencies: String?,
                          success: (Array<Currency>?) -> Unit,
                          error: ((Int, String) -> Unit)? = null,
                          finally: (() -> Unit)? = null) {
        if(currencies == null && publicCurrencies.isNotEmpty()) {
            success.invoke(publicCurrencies)
        }else {
            doRequest(scope, {
                api.publicCurrencyGet(currencies).apply {
                    if(currencies == null) {
                        publicCurrencies = this.body() ?: arrayOf()
                    }
                }
            }, successBlock = success, errorBlock = error, finallyBlock = finally)
        }
    }

    fun publicOrderbookGet(scope: CoroutineScope, symbols: String, limit: Int,
                           success: (Any?) -> Unit, error: (Int, String) -> Unit, finally: () -> Unit) {
        doRequest(scope, {
            api.publicOrderbookGet(symbols, limit)
        }, successBlock = success, errorBlock = error, finallyBlock = finally)
    }


    fun publicOrderbookSymbolGet(scope: CoroutineScope, symbol: String, limit: Int, volume: java.math.BigDecimal,
                                 success: (Orderbook?) -> Unit, error: (Int, String) -> Unit, finally: () -> Unit) {

        doRequest(scope, {
            api.publicOrderbookSymbolGet(symbol, limit, volume)
        }, successBlock = success, errorBlock = error, finallyBlock = finally)
    }

    fun publicSymbolGet(scope: CoroutineScope, symbols: String?,
                        success: (Array<Symbol>?) -> Unit, error: (Int, String) -> Unit, finally: () -> Unit) {
        doRequest(scope, {
            api.publicSymbolGet(symbols)
        }, successBlock = success, errorBlock = error, finallyBlock = finally)
    }

    fun publicSymbolSymbolGet(scope: CoroutineScope, symbol: String,
                              success: (Symbol?) -> Unit, error: (Int, String) -> Unit, finally: () -> Unit) {
        doRequest(scope, {
            api.publicSymbolSymbolGet(symbol)
        }, successBlock = success, errorBlock = error, finallyBlock = finally)
    }

    fun publicTickerGet(scope: CoroutineScope,
                        symbols: String?,
                        success: (Array<Ticker>?) -> Unit, error: (Int, String) -> Unit, finally: () -> Unit) {
        doRequest(scope, {
            api.publicTickerGet(symbols)
        }, successBlock = success, errorBlock = error, finallyBlock = finally)
    }

    fun publicTickerSymbolGet(scope: CoroutineScope,
                              symbol: String,
                              success: (Ticker?) -> Unit, error: (Int, String) -> Unit, finally: () -> Unit) {
        doRequest(scope, {
            api.publicTickerSymbolGet(symbol)
        }, successBlock = success, errorBlock = error, finallyBlock = finally)
    }


    fun publicTradesGet(scope: CoroutineScope, symbols: String, sort: String, from: String, till: String, limit: Int, offset: Int,
                        success: (Any?) -> Unit, error: (Int, String) -> Unit, finally: () -> Unit) {
        doRequest(scope, {
            api.publicTradesGet(symbols, sort, from, till, limit, offset)
        }, successBlock = success, errorBlock = error, finallyBlock = finally)
    }

    fun publicTradesSymbolGet(scope: CoroutineScope, symbol: String, sort: String, by: String, from: String, till: String, limit: Int, offset: Int,
                              success: (Array<PublicTrade>?) -> Unit, error: (Int, String) -> Unit, finally: () -> Unit) {
        doRequest(scope, {
            api.publicTradesSymbolGet(symbol, sort, by, from, till, limit, offset)
        }, successBlock = success, errorBlock = error, finallyBlock = finally)
    }

}