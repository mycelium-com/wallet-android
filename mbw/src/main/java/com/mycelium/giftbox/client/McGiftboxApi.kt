package com.mycelium.giftbox.client

import com.mycelium.giftbox.client.model.MCOrderStatusRequest
import com.mycelium.giftbox.client.model.MCCreateOrderRequest
import com.mycelium.giftbox.client.model.MCOrderResponse
import com.mycelium.giftbox.client.model.MCOrderStatusResponse
import com.mycelium.giftbox.client.model.MCProductInfo
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface McGiftboxApi {

    @GET("brands")
    suspend fun products(): Response<List<MCProductInfo>>

    @POST("orders")
    suspend fun createOrder(@Body body: MCCreateOrderRequest): Response<MCOrderResponse>


    @POST("order-status")
    suspend fun orderStatus(@Body body: MCOrderStatusRequest): Response<MCOrderStatusResponse>

//    @GET("product")
//    suspend fun product(
//        @Query("client_user_id") clientUserId: String,
//        @Query("client_order_id") clientOrderId: String,
//        @Query("code") productId: String
//    ): Response<ProductResponse>
//
//    @GET("price")
//    suspend fun price(
//        @Query("client_user_id") clientUserId: String,
//        @Query("client_order_id") clientOrderId: String,
//        @Query("amount") amount: Int,
//        @Query("quantity") quantity: Int,
//        @Query("code") code: String,
//        @Query("currency_id") currencyId: String? = null
//    ): Response<PriceResponse>
//
//
//    @GET("checkout-product")
//    suspend fun checkoutProduct(
//        @Query("client_user_id") clientUserId: String,
//        @Query("client_order_id") clientOrderId: String,
//        @Query("code") code: String,
//        @Query("quantity") quantity: Int,
//        @Query("amount") amount: Int,
//        @Query("currency_id") currencyId: String? = null
//    ): Response<CheckoutProductResponse>
//
//    @POST("create-order")
//    suspend fun createOrder(
//        @Body createOrderBody: CreateOrderRequest
//    ): Response<OrderResponse>
//
//    @GET("orders")
//    suspend fun orders(
//        @Query("client_user_id") clientUserId: String,
//        @Query("offset") offset: Long? = null,
//        @Query("limit") limit: Long? = null
//    ): Response<OrdersHistoryResponse>
//
//    @GET("get-order")
//    suspend fun order(
//        @Query("client_user_id") clientUserId: String,
//        @Query("client_order_id") clientOrderId: String
//    ): Response<OrderResponse>


    companion object {
        fun create(): McGiftboxApi = createApi(GiftboxConstants.MC_ENDPOINT)
    }
}
