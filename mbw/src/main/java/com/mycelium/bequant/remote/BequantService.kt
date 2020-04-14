package com.mycelium.bequant.remote

import com.mycelium.bequant.remote.model.Auth
import com.mycelium.bequant.remote.model.AuthResponse
import com.mycelium.bequant.remote.model.Register
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST


interface BequantService {
    @POST("account/register")
    fun register(@Body register: Register): Call<Void>

    @POST("account/auth")
    fun authorize(@Body auth: Auth): Call<AuthResponse>

    @POST("account/totp/create")
    fun totpCreate()

    @POST("account/password/reset")
    fun resetPassword(): Call<Register>

    @GET("public/currency")
    fun currencies()
}