package com.mycelium.bequant.remote.repositories

import android.app.Activity
import com.google.gson.Gson
import com.mycelium.bequant.Constants
import com.mycelium.bequant.Constants.LAST_SYMBOLS_UPDATE
import com.mycelium.bequant.remote.doRequest
import com.mycelium.bequant.remote.trading.api.PublicApi
import com.mycelium.bequant.remote.trading.model.*
import com.mycelium.bequant.remote.trading.model.Currency
import com.mycelium.wallet.WalletApplication
import kotlinx.coroutines.CoroutineScope
import java.util.*
import java.util.concurrent.TimeUnit

class PublicApiRespository {

    private val api = PublicApi.create()
    private val preference by lazy { WalletApplication.getInstance().getSharedPreferences(Constants.PUBLIC_REPOSITORY, Activity.MODE_PRIVATE) }


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
        if (currencies == null && publicCurrencies.isNotEmpty()) {
            success.invoke(publicCurrencies)
            finally?.invoke()
        } else {
            doRequest(scope, {
                api.publicCurrencyGet(currencies).apply {
                    if (currencies == null) {
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

    private var publicSymbols: Array<Symbol>
        get() = Gson().fromJson(preference.getString("symbols", "[]"), Array<Symbol>::class.java)
        set(value) {
            preference.edit()
                    .putString("symbols", Gson().toJson(value))
                    .putLong(LAST_SYMBOLS_UPDATE, Date().time)
                    .apply()
        }

    fun publicSymbolGet(scope: CoroutineScope, symbols: String?,
                        success: (Array<Symbol>?) -> Unit, error: (Int, String) -> Unit, finally: () -> Unit) {
        if (publicSymbols.isNotEmpty()
                && TimeUnit.MILLISECONDS.toDays(Date().time - preference.getLong(LAST_SYMBOLS_UPDATE, 0)) < 7) {
            if (symbols == null) {
                success.invoke(publicSymbols)
            } else {
                success.invoke(publicSymbols.filter { symbols.contains(it.id) }.toTypedArray())
            }
            finally.invoke()
        } else {
            doRequest(scope, {
                api.publicSymbolGet(symbols).apply {
                    if (symbols == null) {
                        publicSymbols = this.body() ?: arrayOf()
                    }
                }
            }, successBlock = success, errorBlock = error, finallyBlock = finally)
        }
    }

    fun publicSymbolSymbolGet(scope: CoroutineScope, symbol: String,
                              success: (Symbol?) -> Unit, error: (Int, String) -> Unit, finally: () -> Unit) {
        if (publicSymbols.isNotEmpty()) {
            success.invoke(publicSymbols.find { it.id == symbol })
            finally.invoke()
        } else {
            doRequest(scope, {
                api.publicSymbolSymbolGet(symbol)
            }, successBlock = success, errorBlock = error, finallyBlock = finally)
        }
    }

    fun publicTickerGet(scope: CoroutineScope,
                        symbols: String?,
                        success: (Array<Ticker>?) -> Unit, error: (Int, String) -> Unit, finally: () -> Unit) {
        publicSymbolGet(scope, null, {
            doRequest(scope, {
                api.publicTickerGet(symbols)
            }, successBlock = success, errorBlock = error, finallyBlock = finally)
        }, { code, msg ->
            error.invoke(code, msg)
            finally.invoke()
        }, {})
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