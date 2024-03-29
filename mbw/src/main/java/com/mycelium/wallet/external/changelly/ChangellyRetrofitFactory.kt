package com.mycelium.wallet.external.changelly

import com.mycelium.wallet.BuildConfig
import com.mycelium.wallet.UserKeysManager
import com.mycelium.wallet.external.DigitalSignatureInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.net.ssl.SSLContext

object ChangellyRetrofitFactory {
    private const val BASE_URL = "https://changelly-viper.mycelium.com/v2/"

    private val userKeyPair = UserKeysManager.userSignKeys

    private fun getHttpClient(): OkHttpClient {
        val sslContext = SSLContext.getInstance("TLSv1.3")
        sslContext.init(null, null, null)
        return OkHttpClient.Builder().apply {
            // sslSocketFactory uses system defaults X509TrustManager, so deprecation suppressed
            // referring to sslSocketFactory(SSLSocketFactory, X509TrustManager) docs:
            /**
             * Most applications should not call this method, and instead use the system defaults.
             * Those classes include special optimizations that can be lost
             * if the implementations are decorated.
             */
            @Suppress("DEPRECATION") sslSocketFactory(sslContext.socketFactory)
            addInterceptor(ChangellyInterceptor())
            addInterceptor(DigitalSignatureInterceptor(userKeyPair))
            if (!BuildConfig.DEBUG) return@apply
            addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        }.build()
    }


    val api: ChangellyAPIService =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(getHttpClient())
            .build()
            .create(ChangellyAPIService::class.java)
}

