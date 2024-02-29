package com.mycelium.wallet.external.vip

import com.mycelium.wallet.BuildConfig
import com.mycelium.wallet.external.DigitalSignatureInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class VipRetrofitFactory(private val keyPair: AsymmetricCipherKeyPair) {
    private val httpClient = OkHttpClient.Builder()
        .apply {
            addInterceptor(DigitalSignatureInterceptor(keyPair))
            if (!BuildConfig.DEBUG) return@apply
            addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        }.build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://google.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .client(httpClient)
        .build()

    fun createApi(): VipAPI = retrofit.create(VipAPI::class.java)
}
