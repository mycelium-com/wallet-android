package com.mycelium.bequant.remote

import com.mycelium.bequant.remote.model.*
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query


interface BequantRegService {
    @POST("account/register")
    fun register(@Body register: Register): Call<Void>

    @GET("account/email/confirm")
    fun emailConfirm(@Query("token") confirmToken: String): Call<BequantResponse?>

    @POST("account/email/confirm/resend")
    fun resendRegister(@Body email: Email): Call<Void>

    @GET("account/totp/list")
    fun totpList(): Call<TotpListResponse>

    @POST("account/totp/create")
    fun totpCreate(): Call<TotpCreateResponse?>

    @POST("account/totp/activate")
    fun totpActivate(@Body activate: TotpActivate): Call<AuthResponse?>

    @GET("account/totp/confirm")
    fun totpConfirm(@Query("token") token: String): Call<TotpConfirmResponse>

    @POST("account/auth")
    fun authorize(@Body auth: Auth): Call<AuthResponse>

    @POST("api-key")
    fun getApiKey(): Call<ApiKeyResponse>

    @POST("account/password/reset")
    fun resetPassword(@Body password: Email): Call<RegisterResponse>

    @GET("account/password/set")
    fun resetPasswordSet(@Query("token") token: String): Call<BequantResponse?>

    @POST("account/password/set")
    fun resetPasswordSet(@Body passwordSet: PasswordSet): Call<RegisterResponse>

    @POST("account/password/update")
    fun passwordUpdate(@Body passwordUpdate: PasswordUpdate): Call<RegisterResponse>
}