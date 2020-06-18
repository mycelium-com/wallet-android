package com.mycelium.bequant.remote

import com.mycelium.bequant.remote.model.BequantBalance
import com.mycelium.bequant.remote.model.Currency
import com.mycelium.bequant.remote.model.DepositAddress
import com.mycelium.bequant.remote.model.Ticker
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path


interface BequantApiService {

    @GET("api/2/public/currency")
    fun currencies(): Call<List<Currency>>

    @GET("api/2/account/balance")
    fun balance(): Call<List<BequantBalance>>

    @GET("api/2/public/ticker")
    fun tickers(): Call<List<Ticker>>

    @GET("api/2/account/crypto/address/{currency}")
    fun depositAddress(@Path("currency") currency: String): Call<DepositAddress>

    @POST("api/2/account/crypto/address/{currency}")
    fun createDepositAddress(@Path("currency") currency: String): Call<DepositAddress>

}