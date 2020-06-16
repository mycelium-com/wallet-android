package com.mycelium.bequant.remote.trading.api

import com.mycelium.bequant.remote.trading.model.*
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PublicApi {
    @GET("/public/candles")
    fun publicCandlesGet(@Query("symbols") symbols: kotlin.String, @Query("period") period: kotlin.String, @Query("sort") sort: kotlin.String, @Query("from") from: kotlin.String, @Query("till") till: kotlin.String, @Query("limit") limit: kotlin.Int, @Query("offset") offset: kotlin.Int): Response<kotlin.Any>

    @GET("/public/candles/{symbol}")
    fun publicCandlesSymbolGet(@Path("symbol") symbol: kotlin.String, @Query("period") period: kotlin.String, @Query("sort") sort: kotlin.String, @Query("from") from: kotlin.String, @Query("till") till: kotlin.String, @Query("limit") limit: kotlin.Int, @Query("offset") offset: kotlin.Int): Response<kotlin.Array<Candle>>

    @GET("/public/currency/{currency}")
    fun publicCurrencyCurrencyGet(@Path("currency") currency: kotlin.String): Response<Currency>

    @GET("/public/currency")
    fun publicCurrencyGet(@Query("currencies") currencies: kotlin.String): Response<kotlin.Array<Currency>>

    @GET("/public/orderbook")
    fun publicOrderbookGet(@Query("symbols") symbols: kotlin.String, @Query("limit") limit: kotlin.Int): Response<kotlin.Any>

    @GET("/public/orderbook/{symbol}")
    fun publicOrderbookSymbolGet(@Path("symbol") symbol: kotlin.String, @Query("limit") limit: kotlin.Int, @Query("volume") volume: java.math.BigDecimal): Response<Orderbook>

    @GET("/public/symbol")
    fun publicSymbolGet(@Query("symbols") symbols: kotlin.String): Response<kotlin.Array<Symbol>>

    @GET("/public/symbol/{symbol}")
    fun publicSymbolSymbolGet(@Path("symbol") symbol: kotlin.String): Response<Symbol>

    @GET("/public/ticker")
    fun publicTickerGet(@Query("symbols") symbols: kotlin.String): Response<kotlin.Array<Ticker>>

    @GET("/public/ticker/{symbol}")
    fun publicTickerSymbolGet(@Path("symbol") symbol: kotlin.String): Response<Ticker>

    @GET("/public/trades")
    fun publicTradesGet(@Query("symbols") symbols: kotlin.String, @Query("sort") sort: kotlin.String, @Query("from") from: kotlin.String, @Query("till") till: kotlin.String, @Query("limit") limit: kotlin.Int, @Query("offset") offset: kotlin.Int): Response<kotlin.Any>

    @GET("/public/trades/{symbol}")
    fun publicTradesSymbolGet(@Path("symbol") symbol: kotlin.String, @Query("sort") sort: kotlin.String, @Query("by") by: kotlin.String, @Query("from") from: kotlin.String, @Query("till") till: kotlin.String, @Query("limit") limit: kotlin.Int, @Query("offset") offset: kotlin.Int): Response<kotlin.Array<PublicTrade>>

}
