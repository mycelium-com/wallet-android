package com.mycelium.giftbox.client

import retrofit2.Response
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Query

interface GiftboxApi {

    @GET("products")
    @FormUrlEncoded
    suspend fun products(
            @Query("search") search: String,
            @Query("country") country: String,
            @Query("category") category: String,
            @Query("product_id") product_id: String,
            @Query("offset") offset: Long,
            @Query("limit") limit: Long,
            @Query("client_user_id") clientUserId: String,
            @Query("client_order_id") client_order_id: String
    ): Response<ProductsResponse>

    @GET("product")
    @FormUrlEncoded
    suspend fun product(
            @Query("client_user_id") clientUserId: String,
            @Query("client_order_id") client_order_id: String,
            @Query("product_id") productId: String
    ): Response<ProductResponse>

    @GET("price")
    @FormUrlEncoded
    suspend fun price(
            @Query("client_user_id") clientUserId: String,
            @Query("client_order_id") client_order_id: String,
            @Query("code") code: String,
            @Query("quantity") quantity: Int,
            @Query("amount") amount: Int,
            @Query("currency_id") currencyId: String
    ): Response<PriceResponse>

    companion object {
        fun create(): GiftboxApi = createApi()
    }
}
