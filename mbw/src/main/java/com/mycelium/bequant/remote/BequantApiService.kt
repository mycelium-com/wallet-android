package com.mycelium.bequant.remote

import com.mycelium.bequant.remote.model.BequantBalance
import retrofit2.Call
import retrofit2.http.GET


interface BequantApiService {
    @GET("api/2/account/balance")
    fun balance(): Call<List<BequantBalance>>

}