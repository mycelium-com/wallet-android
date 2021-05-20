package com.mycelium.giftbox.client

import com.mycelium.giftbox.client.models.*
import kotlinx.coroutines.flow.Flow
import retrofit2.Response
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Query

interface GiftboxApi {

    @GET("products")
    @FormUrlEncoded
    suspend fun products(
        @Query("search") search: String? = null,
        @Query("country") country: String? = null,
        @Query("category") category: String? = null,
        @Query("product_id") product_id: String? = null,
        @Query("offset") offset: Long,
        @Query("limit") limit: Long,
        @Query("client_user_id") clientUserId: String,
        @Query("client_order_id") client_order_id: String
    ): Flow<ProductsResponse>

    @GET("product")
    @FormUrlEncoded
    suspend fun product(
        @Query("client_user_id") clientUserId: String,
        @Query("client_order_id") client_order_id: String,
        @Query("product_id") productId: String
    ): Flow<ProductResponse>

    @GET("price")
    @FormUrlEncoded
    suspend fun price(
        @Query("client_user_id") clientUserId: String,
        @Query("client_order_id") client_order_id: String,
        @Query("code") code: String,
        @Query("quantity") quantity: Int,
        @Query("amount") amount: Int,
        @Query("currency_id") currencyId: String
    ): Flow<PriceResponse>


    @GET("checkoutProduct")
    @FormUrlEncoded
    suspend fun checkoutProduct(
        @Query("client_user_id") clientUserId: String,
        @Query("client_order_id") client_order_id: String,
        @Query("code") code: String,
        @Query("quantity") quantity: Int,
        @Query("amount") amount: Int
    ): Flow<CheckoutProductResponse>

    @GET("createOrder")
    @FormUrlEncoded
    suspend fun createOrder(
        @Query("client_user_id") clientUserId: String,
        @Query("client_order_id") client_order_id: String,
        @Query("code") code: String,
        @Query("quantity") quantity: Int,
        @Query("amount") amount: Int,
        @Query("currency_id") currencyId: String
    ): Flow<CheckoutProductResponse>

    @GET("getOrder")
    @FormUrlEncoded
    suspend fun getOrder(
        @Query("client_user_id") clientUserId: String,
        @Query("client_order_id") client_order_id: String
    ): Flow<GetOrderResponse>

    companion object {
        fun create(): GiftboxApi = createApi()
    }
}
