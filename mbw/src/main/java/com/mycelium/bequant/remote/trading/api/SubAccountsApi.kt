package com.mycelium.bequant.remote.trading.api

import com.mycelium.bequant.remote.client.createApi
import com.mycelium.bequant.remote.trading.model.*
import retrofit2.Response
import retrofit2.http.*

interface SubAccountsApi {
    @GET("sub-acc/acl")
    suspend fun subAccAclGet(@Query("subAccountIds") subAccountIds: kotlin.String): Response<InlineResponse2003>

    @FormUrlEncoded
    @PUT("sub-acc/acl/:subAccountUserID")
    suspend fun subAccAclSubAccountUserIDPut(@Path("subAccountUserID") subAccountUserID: kotlin.String, @Field("description") description: kotlin.String, @Field("isPayoutEnabled") isPayoutEnabled: kotlin.Boolean): Response<InlineResponse2003>

    @FormUrlEncoded
    @POST("sub-acc/activate")
    suspend fun subAccActivatePost(@Field("ids") ids: kotlin.String): Response<InlineResponse2001>

    @GET("sub-acc/balance/:subAccountUserID")
    suspend fun subAccBalanceSubAccountUserIDGet(@Path("subAccountUserID") subAccountUserID: kotlin.Int): Response<InlineResponse2004>

    @GET("sub-acc/deposit-address/:subAccountUserID/:currency")
    suspend fun subAccDepositAddressSubAccountUserIDCurrencyGet(@Path("subAccountUserID") subAccountUserID: kotlin.Int, @Path("currency") currency: kotlin.String): Response<Address>

    @FormUrlEncoded
    @POST("sub-acc/freeze")
    suspend fun subAccFreezePost(@Field("ids") ids: kotlin.String): Response<InlineResponse2001>

    @GET("sub-acc")
    suspend fun subAccGet(): Response<kotlin.Array<SubAccount>>

    @FormUrlEncoded
    @POST("sub-acc/transfer")
    suspend fun subAccTransferPost(@Field("subAccountId") subAccountId: kotlin.Int, @Field("amount") amount: kotlin.String, @Field("currency") currency: kotlin.String, @Field("type") type: kotlin.String): Response<InlineResponse2002>


    companion object {
        fun create() = createApi<SubAccountsApi>()
    }
}
