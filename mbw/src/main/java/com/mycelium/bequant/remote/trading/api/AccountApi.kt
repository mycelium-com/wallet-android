package com.mycelium.bequant.remote.trading.api

import com.mycelium.bequant.remote.client.createApi
import com.mycelium.bequant.remote.trading.model.*
import retrofit2.Response
import retrofit2.http.*

interface AccountApi {
    @GET("account/balance")
    suspend fun accountBalanceGet(): Response<Array<Balance>>

    @GET("account/crypto/address/{currency}")
    suspend fun accountCryptoAddressCurrencyGet(
            @Path("currency") currency: String): Response<Address>

    @POST("account/crypto/address/{currency}")
    suspend fun accountCryptoAddressCurrencyPost(
            @Path("currency") currency: String): Response<Address>

    @FormUrlEncoded
    @POST("account/crypto/check-offchain-available")
    suspend fun accountCryptoCheckOffchainAvailablePost(
            @Field("currency") currency: String,
            @Field("address") address: String,
            @Field("paymentId") paymentId: String): Response<WithdrawConfirm>

    @GET("account/crypto/is-mine/{address}")
    suspend fun accountCryptoIsMineAddressGet(
            @Path("address") address: String): Response<AddressIsMineCheck>

    @FormUrlEncoded
    @POST("account/crypto/transfer-convert")
    suspend fun accountCryptoTransferConvertPost(
            @Field("fromCurrency") fromCurrency: String,
            @Field("toCurrency") toCurrency: String,
            @Field("amount") amount: String): Response<Array<String>>

    @DELETE("account/crypto/withdraw/{id}")
    suspend fun accountCryptoWithdrawIdDelete(
            @Path("id") id: String): Response<WithdrawConfirm>

    @PUT("account/crypto/withdraw/{id}")
    suspend fun accountCryptoWithdrawIdPut(
            @Path("id") id: String): Response<WithdrawConfirm>

    @FormUrlEncoded
    @POST("account/crypto/withdraw")
    suspend fun accountCryptoWithdrawPost(
            @Field("currency") currency: String,
            @Field("amount") amount: String,
            @Field("address") address: String,
            @Field("paymentId") paymentId: String?,
            @Field("includeFee") includeFee: Boolean?,
            @Field("autoCommit") autoCommit: Boolean?,
            @Field("useOffchain") useOffchain: String?): Response<InlineResponse200>

    @GET("account/transactions")
    suspend fun accountTransactionsGet(
            @Query("currency") currency: String,
            @Query("sort") sort: String,
            @Query("by") by: String,
            @Query("from") from: String,
            @Query("till") till: String,
            @Query("limit") limit: Int,
            @Query("offset") offset: Int): Response<Array<Transaction>>

    @GET("account/transactions/{id}")
    suspend fun accountTransactionsIdGet(@Path("id") id: String): Response<Transaction>

    @FormUrlEncoded
    @POST("account/transfer")
    suspend fun accountTransferPost(
            @Field("currency") currency: String,
            @Field("amount") amount: String,
            @Field("type") type: String): Response<InlineResponse200>

    companion object {
        fun create(): AccountApi = createApi()
    }
}
