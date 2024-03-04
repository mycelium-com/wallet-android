package com.mycelium.wallet.external.vip

import com.mycelium.wallet.BuildConfig
import com.mycelium.wallet.external.DigitalSignatureInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class VipRetrofitFactory(private val keyPair: AsymmetricCipherKeyPair) {
    private companion object {
        const val baseUrl = "https://bb60-2001-41d0-701-1100-00-21da.ngrok-free.app/api/v1/vip-codes/"
    }

    private val httpClient = OkHttpClient.Builder()
        .apply {
            addInterceptor(DigitalSignatureInterceptor(keyPair))
            if (!BuildConfig.DEBUG) return@apply
            addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        }.build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .client(httpClient)
        .build()

    fun createApi(): VipAPI = retrofit.create(VipAPI::class.java)
}
