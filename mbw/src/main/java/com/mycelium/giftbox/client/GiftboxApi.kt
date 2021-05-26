package com.mycelium.giftbox.client

import com.mycelium.giftbox.client.models.*
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

//curl -s -v -G -H 'Content-Type: application/json' -H 'Authorization: Basic VEFrbmdDVkRiRURJMHluSTNsNzBnb1Vya1l0eHFiNW46Y2dwQ3NlOVVaaXFQcHg1cmRFUDhONW53akJ5NG8xc2s=' --data-urlencode 'search=' --data-urlencode 'country=RU' --data-urlencode 'offset=0' --data-urlencode 'limit=10' 'https://apps-api.giftbox.tech/api/products'

interface GiftboxApi {
    @GET("products")
    suspend fun products(
        @Query("search") search: String? = null,
        @Query("country") country: String? = null,
        @Query("category") category: String? = null,
        @Query("offset") offset: Long? = null,
        @Query("limit") limit: Long? = null,
        @Query(value = "client_user_id") clientUserId: String,
        @Query(value = "client_order_id") clientOrderId: String?
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
        @Query("code") code: String,
        @Query("quantity") quantity: Int,
        @Query("amount") amount: Int,
        @Query("currency_id") currencyId: String
    ): Response<PriceResponse>


    @GET("checkoutProduct")
    suspend fun checkoutProduct(
        @Query("client_user_id") clientUserId: String,
        @Query("client_order_id") clientOrderId: String,
        @Query("code") code: String,
        @Query("quantity") quantity: Int,
        @Query("amount") amount: Int
    ): Response<CheckoutProductResponse>

    @POST("createOrder")
    suspend fun createOrder(
        @Query("client_user_id") clientUserId: String,
        @Query("client_order_id") clientOrderId: String,
        @Query("code") code: String,
        @Query("quantity") quantity: Int,
        @Query("amount") amount: Int,
        @Query("currency_id") currencyId: String
    ): Response<CreateOrderResponse>

    @GET("orders")
    suspend fun orders(
        @Query("client_user_id") clientUserId: String,
        @Query("offset") offset: Long? = null,
        @Query("limit") limit: Long? = null
    ): Response<GetOrdersResponse>

    @GET("get-order")
    suspend fun order(
        @Query("client_user_id") clientUserId: String,
        @Query("client_order_id") clientOrderId: String
    ): Response<GetOrderResponse>


    companion object {
        fun create(): GiftboxApi = createApi()
    }
}
