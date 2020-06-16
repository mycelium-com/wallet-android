package com.mycelium.bequant.remote.trading.api

import com.mycelium.bequant.remote.trading.model.Balance
import com.mycelium.bequant.remote.trading.model.Order
import com.mycelium.bequant.remote.trading.model.TradingFee
import retrofit2.Response
import retrofit2.http.*

interface TradingApi {
    @DELETE("/order/{clientOrderId}")
    fun orderClientOrderIdDelete(@Path("clientOrderId") clientOrderId: kotlin.String): Response<Order>

    @GET("/order/{clientOrderId}")
    fun orderClientOrderIdGet(@Path("clientOrderId") clientOrderId: kotlin.String, @Query("wait") wait: kotlin.Int): Response<Order>

    @FormUrlEncoded
    @PATCH("/order/{clientOrderId}")
    fun orderClientOrderIdPatch(@Path("clientOrderId") clientOrderId: kotlin.String, @Field("quantity") quantity: kotlin.String, @Field("requestClientId") requestClientId: kotlin.String, @Field("price") price: kotlin.String): Response<Order>

    @FormUrlEncoded
    @PUT("/order/{clientOrderId}")
    fun orderClientOrderIdPut(@Path("clientOrderId") clientOrderId: kotlin.String, @Field("symbol") symbol: kotlin.String, @Field("side") side: kotlin.String, @Field("timeInForce") timeInForce: kotlin.String, @Field("quantity") quantity: kotlin.String, @Field("type") type: kotlin.String, @Field("price") price: kotlin.String, @Field("stopPrice") stopPrice: kotlin.String, @Field("expireTime") expireTime: java.time.OffsetDateTime, @Field("strictValidate") strictValidate: kotlin.Boolean, @Field("postOnly") postOnly: kotlin.Boolean): Response<Order>

    @FormUrlEncoded
    @DELETE("/order")
    fun orderDelete(@Field("symbol") symbol: kotlin.String): Response<kotlin.Array<Order>>

    @GET("/order")
    fun orderGet(@Query("symbol") symbol: kotlin.String): Response<kotlin.Array<Order>>

    @FormUrlEncoded
    @POST("/order")
    fun orderPost(@Field("symbol") symbol: kotlin.String, @Field("side") side: kotlin.String, @Field("quantity") quantity: kotlin.String, @Field("clientOrderId") clientOrderId: kotlin.String, @Field("type") type: kotlin.String, @Field("timeInForce") timeInForce: kotlin.String, @Field("price") price: kotlin.String, @Field("stopPrice") stopPrice: kotlin.String, @Field("expireTime") expireTime: java.time.OffsetDateTime, @Field("strictValidate") strictValidate: kotlin.Boolean, @Field("postOnly") postOnly: kotlin.Boolean): Response<Order>

    @GET("/trading/balance")
    fun tradingBalanceGet(): Response<kotlin.Array<Balance>>

    @GET("/trading/fee/{symbol}")
    fun tradingFeeSymbolGet(@Path("symbol") symbol: kotlin.String): Response<TradingFee>

}
