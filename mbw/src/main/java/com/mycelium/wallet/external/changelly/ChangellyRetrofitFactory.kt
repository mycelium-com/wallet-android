package com.mycelium.wallet.external.changelly

import com.mycelium.wallet.BuildConfig
import com.mycelium.wallet.UserKeysManager
import com.mycelium.wallet.external.DigitalSignatureInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext

object ChangellyRetrofitFactory {

    private val userKeyPair by lazy { UserKeysManager.userSignKeys }

    private fun getViperHttpClient(): OkHttpClient {
        val sslContext = SSLContext.getInstance("TLSv1.3")
        sslContext.init(null, null, null)
        return OkHttpClient.Builder().apply {
            connectTimeout(3, TimeUnit.SECONDS)
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

    private fun getChangellyHttpClient(): OkHttpClient {
        return OkHttpClient.Builder().apply {
            addInterceptor(ChangellyInterceptor())
            if (!BuildConfig.DEBUG) return@apply
            addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        }.build()
    }

    val viperApi: ChangellyAPIService by lazy {
        Retrofit.Builder()
            .baseUrl(ExchangeKeys.VIPER_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(getViperHttpClient())
            .build()
            .create(ChangellyAPIService::class.java)
    }

    val changellyApi: ChangellyAPIService =
        Retrofit.Builder()
            .baseUrl(ExchangeKeys.CHANGELLY_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(getChangellyHttpClient())
            .build()
            .create(ChangellyAPIService::class.java)
}
