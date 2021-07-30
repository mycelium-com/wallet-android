package com.mycelium.giftbox.client

import com.mycelium.giftbox.client.models.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

//curl -s -v -G -H 'Content-Type: application/json' -H 'Authorization: Basic VEFrbmdDVkRiRURJMHluSTNsNzBnb1Vya1l0eHFiNW46Y2dwQ3NlOVVaaXFQcHg1cmRFUDhONW53akJ5NG8xc2s=' --data-urlencode 'search=' --data-urlencode 'country=RU' --data-urlencode 'offset=0' --data-urlencode 'limit=10' 'https://apps-api.giftbox.tech/api/products'

interface GiftboxApi {
    @GET("products")
    suspend fun products(
        @Query(value = "client_user_id") clientUserId: String,
        @Query(value = "client_order_id") clientOrderId: String?,
        @Query("category") category: String? = null,
        @Query("search") search: String? = null,
        @Query("country") country: String? = null,
        @Query("offset") offset: Long? = null,
        @Query("limit") limit: Long? = null
    ): Response<ProductsResponse>

    @GET("product")
    suspend fun product(
        @Query("client_user_id") clientUserId: String,
        @Query("client_order_id") clientOrderId: String,
        @Query("code") productId: String
    ): Response<ProductResponse>

    @GET("price")
    suspend fun price(
        @Query("client_user_id") clientUserId: String,
        @Query("client_order_id") clientOrderId: String,
        @Query("amount") amount: Int,
        @Query("quantity") quantity: Int,
        @Query("code") code: String,
        @Query("currency_id") currencyId: String? = null
    ): Response<PriceResponse>


    @GET("checkout-product")
    suspend fun checkoutProduct(
        @Query("client_user_id") clientUserId: String,
        @Query("client_order_id") clientOrderId: String,
        @Query("code") code: String,
        @Query("quantity") quantity: Int,
        @Query("amount") amount: Int,
        @Query("currency_id") currencyId: String? = null
    ): Response<CheckoutProductResponse>

    @POST("create-order")
    suspend fun createOrder(
        @Body createOrderBody: CreateOrderRequest
    ): Response<OrderResponse>

    @GET("orders")
    suspend fun orders(
        @Query("client_user_id") clientUserId: String,
        @Query("offset") offset: Long? = null,
        @Query("limit") limit: Long? = null
    ): Response<OrdersHistoryResponse>

    @GET("get-order")
    suspend fun order(
        @Query("client_user_id") clientUserId: String,
        @Query("client_order_id") clientOrderId: String
    ): Response<OrderResponse>


    companion object {
        fun create(): GiftboxApi = createApi()
    }
}
