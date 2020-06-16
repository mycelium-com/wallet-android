package com.mycelium.bequant.remote.trading.api

import com.mycelium.bequant.remote.trading.model.Order
import com.mycelium.bequant.remote.trading.model.Trade
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TradingHistoryApi {
    @GET("/history/order")
    fun historyOrderGet(@Query("symbol") symbol: kotlin.String, @Query("from") from: kotlin.String, @Query("till") till: kotlin.String, @Query("limit") limit: kotlin.Int, @Query("offset") offset: kotlin.Int, @Query("clientOrderId") clientOrderId: kotlin.String): Response<Array<Order>>

    @GET("/history/order/{id}/trades")
    fun historyOrderIdTradesGet(@Path("id") id: kotlin.Int): Response<kotlin.Array<Trade>>

    @GET("/history/trades")
    fun historyTradesGet(@Query("symbol") symbol: kotlin.String, @Query("sort") sort: kotlin.String, @Query("by") by: kotlin.String, @Query("from") from: kotlin.String, @Query("till") till: kotlin.String, @Query("limit") limit: kotlin.Int, @Query("offset") offset: kotlin.Int): Response<kotlin.Array<Trade>>

}
