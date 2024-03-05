package com.mycelium.wallet.external.changelly

import com.mycelium.wallet.BuildConfig
import com.mycelium.wallet.UserKeysManager
import com.mycelium.wallet.external.DigitalSignatureInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ChangellyRetrofitFactory {
    private const val BASE_URL = "https://api.changelly.com/v2/"

    private val userKeyPair = UserKeysManager.userSignKeys

    private fun getHttpClient() =
        OkHttpClient.Builder().apply {
            addInterceptor(ChangellyInterceptor())
            addInterceptor(DigitalSignatureInterceptor(userKeyPair))
            if (!BuildConfig.DEBUG) return@apply
            addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        }.build()


    val api: ChangellyAPIService =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(getHttpClient())
            .build()
            .create(ChangellyAPIService::class.java)
}

