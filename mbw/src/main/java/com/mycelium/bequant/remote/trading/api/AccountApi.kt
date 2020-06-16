package com.mycelium.bequant.remote.trading.api

import com.mycelium.bequant.remote.trading.model.*
import retrofit2.Response
import retrofit2.http.*

interface AccountApi {
    @GET("/account/balance")
    suspend fun accountBalanceGet(): Response<Array<Balance>>

    @GET("/account/crypto/address/{currency}")
    suspend fun accountCryptoAddressCurrencyGet(@Path("currency") currency: kotlin.String): Response<Address>

    @POST("/account/crypto/address/{currency}")
    suspend fun accountCryptoAddressCurrencyPost(@Path("currency") currency: kotlin.String): Response<Address>

    @FormUrlEncoded
    @POST("/account/crypto/check-offchain-available")
    suspend fun accountCryptoCheckOffchainAvailablePost(@Field("currency") currency: kotlin.String, @Field("address") address: kotlin.String, @Field("paymentId") paymentId: kotlin.String): Response<WithdrawConfirm>

    @GET("/account/crypto/is-mine/{address}")
    suspend fun accountCryptoIsMineAddressGet(@Path("address") address: kotlin.String): Response<AddressIsMineCheck>

    @FormUrlEncoded
    @POST("/account/crypto/transfer-convert")
    suspend fun accountCryptoTransferConvertPost(@Field("fromCurrency") fromCurrency: kotlin.String, @Field("toCurrency") toCurrency: kotlin.String, @Field("amount") amount: kotlin.String): Response<kotlin.Array<kotlin.String>>

    @DELETE("/account/crypto/withdraw/{id}")
    suspend fun accountCryptoWithdrawIdDelete(@Path("id") id: kotlin.String): Response<WithdrawConfirm>

    @PUT("/account/crypto/withdraw/{id}")
    suspend fun accountCryptoWithdrawIdPut(@Path("id") id: kotlin.String): Response<WithdrawConfirm>

    @FormUrlEncoded
    @POST("/account/crypto/withdraw")
    suspend fun accountCryptoWithdrawPost(@Field("currency") currency: kotlin.String, @Field("amount") amount: kotlin.String, @Field("address") address: kotlin.String, @Field("paymentId") paymentId: kotlin.String, @Field("includeFee") includeFee: kotlin.Boolean, @Field("autoCommit") autoCommit: kotlin.Boolean, @Field("useOffchain") useOffchain: kotlin.String): Response<InlineResponse200>

    @GET("/account/transactions")
    suspend fun accountTransactionsGet(@Query("currency") currency: kotlin.String, @Query("sort") sort: kotlin.String, @Query("by") by: kotlin.String, @Query("from") from: kotlin.String, @Query("till") till: kotlin.String, @Query("limit") limit: kotlin.Int, @Query("offset") offset: kotlin.Int): Response<kotlin.Array<Transaction>>

    @GET("/account/transactions/{id}")
    suspend fun accountTransactionsIdGet(@Path("id") id: kotlin.String): Response<Transaction>

    @FormUrlEncoded
    @POST("/account/transfer")
    suspend fun accountTransferPost(@Field("currency") currency: kotlin.String, @Field("amount") amount: kotlin.String, @Field("type") type: kotlin.String): Response<InlineResponse200>

}
