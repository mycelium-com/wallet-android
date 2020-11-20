package com.mycelium.bequant.remote.trading.api

import com.mycelium.bequant.remote.client.createApi
import com.mycelium.bequant.remote.trading.model.*
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.math.BigDecimal

interface PublicApi {
    @GET("public/candles")
    suspend fun publicCandlesGet(
            @Query("symbols") symbols: String,
            @Query("period") period: String,
            @Query("sort") sort: String,
            @Query("from") from: String,
            @Query("till") till: String,
            @Query("limit") limit: Int,
            @Query("offset") offset: Int): Response<Any>

    @GET("public/candles/{symbol}")
    suspend fun publicCandlesSymbolGet(
            @Path("symbol") symbol: String,
            @Query("period") period: String,
            @Query("sort") sort: String,
            @Query("from") from: String,
            @Query("till") till: String,
            @Query("limit") limit: Int,
            @Query("offset") offset: Int): Response<Array<Candle>>

    @GET("public/currency/{currency}")
    suspend fun publicCurrencyCurrencyGet(
            @Path("currency") currency: String): Response<Currency>

    @GET("public/currency")
    suspend fun publicCurrencyGet(
            @Query("currencies") currencies: String?): Response<Array<Currency>>

    @GET("public/orderbook")
    suspend fun publicOrderbookGet(
            @Query("symbols") symbols: String,
            @Query("limit") limit: Int): Response<Any>

    @GET("public/orderbook/{symbol}")
    suspend fun publicOrderbookSymbolGet(
            @Path("symbol") symbol: String,
            @Query("limit") limit: Int,
            @Query("volume") volume: BigDecimal): Response<Orderbook>

    @GET("public/symbol")
    suspend fun publicSymbolGet(@Query("symbols") symbols: String?): Response<Array<Symbol>>

    @GET("public/symbol/{symbol}")
    suspend fun publicSymbolSymbolGet(@Path("symbol") symbol: String): Response<Symbol>

    @GET("public/ticker")
    suspend fun publicTickerGet(@Query("symbols") symbols: String?): Response<Array<Ticker>>

    @GET("public/ticker/{symbol}")
    suspend fun publicTickerSymbolGet(@Path("symbol") symbol: String): Response<Ticker>

    @GET("public/trades")
    suspend fun publicTradesGet(
            @Query("symbols") symbols: String,
            @Query("sort") sort: String,
            @Query("from") from: String,
            @Query("till") till: String,
            @Query("limit") limit: Int,
            @Query("offset") offset: Int): Response<Any>

    @GET("public/trades/{symbol}")
    suspend fun publicTradesSymbolGet(
            @Path("symbol") symbol: String,
            @Query("sort") sort: String,
            @Query("by") by: String,
            @Query("from") from: String,
            @Query("till") till: String,
            @Query("limit") limit: Int,
            @Query("offset") offset: Int): Response<Array<PublicTrade>>

    companion object {
        fun create() = createApi<PublicApi>()
    }
}
