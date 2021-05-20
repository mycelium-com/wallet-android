package com.mycelium.giftbox.client

import com.mycelium.giftbox.client.models.CheckoutProductResponse
import com.mycelium.giftbox.client.models.PriceResponse
import com.mycelium.giftbox.client.models.ProductResponse
import com.mycelium.giftbox.client.models.ProductsResponse
import retrofit2.Response
import retrofit2.http.*

interface GiftboxApi {

    @GET("products")
    suspend fun products(
//        @Query("search") search: String? = null,
//        @Query("country") country: String? = null,
//        @Query("category") category: String? = null,
//        @Query("product_id") product_id: String? = null,
//        @Query("offset") offset: Long,
//        @Query("limit") limit: Long,
        @Query(value = "client_user_id", encoded = true) clientUserId: String,
        @Query(value = "client_order_id",encoded = true) client_order_id: String?
    ): Response<ProductsResponse>

    @GET("product")
    suspend fun product(
        @Query("client_user_id") clientUserId: String,
        @Query("client_order_id") client_order_id: String,
        @Query("product_id") productId: String
    ): Response<ProductResponse>

    @GET("price")
    suspend fun price(
        @Query("client_user_id") clientUserId: String,
        @Query("client_order_id") client_order_id: String,
        @Query("code") code: String,
        @Query("quantity") quantity: Int,
        @Query("amount") amount: Int,
        @Query("currency_id") currencyId: String
    ): Response<PriceResponse>


    @GET("checkoutProduct")
    suspend fun checkoutProduct(
        @Query("client_user_id") clientUserId: String,
        @Query("client_order_id") client_order_id: String,
        @Query("code") code: String,
        @Query("quantity") quantity: Int,
        @Query("amount") amount: Int
    ): Response<CheckoutProductResponse>

    @POST("createOrder")
    suspend fun createOrder(
        @Query("client_user_id") clientUserId: String,
        @Query("client_order_id") client_order_id: String,
        @Query("code") code: String,
        @Query("quantity") quantity: Int,
        @Query("amount") amount: Int,
        @Query("currency_id") currencyId: String
    ): Response<CheckoutProductResponse>


    companion object {
        fun create(): GiftboxApi = createApi()
    }
}
