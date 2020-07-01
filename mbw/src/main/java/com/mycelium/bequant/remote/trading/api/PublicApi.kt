package com.mycelium.bequant.remote.trading.api

import com.mycelium.bequant.remote.client.createApi
import com.mycelium.bequant.remote.trading.model.*
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PublicApi {
    @GET("public/candles")
    suspend fun publicCandlesGet(@Query("symbols") symbols: kotlin.String, @Query("period") period: kotlin.String, @Query("sort") sort: kotlin.String, @Query("from") from: kotlin.String, @Query("till") till: kotlin.String, @Query("limit") limit: kotlin.Int, @Query("offset") offset: kotlin.Int): Response<kotlin.Any>

    @GET("public/candles/{symbol}")
    suspend fun publicCandlesSymbolGet(@Path("symbol") symbol: kotlin.String, @Query("period") period: kotlin.String, @Query("sort") sort: kotlin.String, @Query("from") from: kotlin.String, @Query("till") till: kotlin.String, @Query("limit") limit: kotlin.Int, @Query("offset") offset: kotlin.Int): Response<kotlin.Array<Candle>>

    @GET("public/currency/{currency}")
    suspend fun publicCurrencyCurrencyGet(@Path("currency") currency: kotlin.String): Response<Currency>

    @GET("public/currency")
    suspend fun publicCurrencyGet(@Query("currencies") currencies: kotlin.String?): Response<kotlin.Array<Currency>>

    @GET("public/orderbook")
    suspend fun publicOrderbookGet(@Query("symbols") symbols: kotlin.String, @Query("limit") limit: kotlin.Int): Response<kotlin.Any>

    @GET("public/orderbook/{symbol}")
    suspend fun publicOrderbookSymbolGet(@Path("symbol") symbol: kotlin.String, @Query("limit") limit: kotlin.Int, @Query("volume") volume: java.math.BigDecimal): Response<Orderbook>

    @GET("public/symbol")
    suspend fun publicSymbolGet(@Query("symbols") symbols: kotlin.String): Response<kotlin.Array<Symbol>>

    @GET("public/symbol/{symbol}")
    suspend fun publicSymbolSymbolGet(@Path("symbol") symbol: kotlin.String): Response<Symbol>

    @GET("public/ticker")
    suspend fun publicTickerGet(@Query("symbols") symbols: kotlin.String?): Response<kotlin.Array<Ticker>>

    @GET("public/ticker/{symbol}")
    suspend fun publicTickerSymbolGet(@Path("symbol") symbol: kotlin.String): Response<Ticker>

    @GET("public/trades")
    suspend fun publicTradesGet(@Query("symbols") symbols: kotlin.String, @Query("sort") sort: kotlin.String, @Query("from") from: kotlin.String, @Query("till") till: kotlin.String, @Query("limit") limit: kotlin.Int, @Query("offset") offset: kotlin.Int): Response<kotlin.Any>

    @GET("public/trades/{symbol}")
    suspend fun publicTradesSymbolGet(@Path("symbol") symbol: kotlin.String, @Query("sort") sort: kotlin.String, @Query("by") by: kotlin.String, @Query("from") from: kotlin.String, @Query("till") till: kotlin.String, @Query("limit") limit: kotlin.Int, @Query("offset") offset: kotlin.Int): Response<kotlin.Array<PublicTrade>>

    companion object {
        fun create() = createApi<PublicApi>()
    }
}
