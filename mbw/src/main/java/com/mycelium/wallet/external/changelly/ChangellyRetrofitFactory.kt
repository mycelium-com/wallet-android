package com.mycelium.wallet.external.changelly

import com.mycelium.wallet.BuildConfig
import com.mycelium.wallet.UserKeysManager
import com.mycelium.wallet.external.DigitalSignatureInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.KeyStore
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

object ChangellyRetrofitFactory {

    private val userKeyPair by lazy { UserKeysManager.userSignKeys }

    private fun getViperHttpClient(): OkHttpClient {
        val sslContext = SSLContext.getInstance("TLSv1.3")

        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(null as KeyStore?)
        val trustManagers = trustManagerFactory.trustManagers
        val trustManager = trustManagers.first { it is X509TrustManager } as X509TrustManager

        sslContext.init(null, arrayOf(trustManager), null)
        return OkHttpClient.Builder().apply {
            connectTimeout(3, TimeUnit.SECONDS)
            // sslSocketFactory uses system defaults X509TrustManager, so deprecation suppressed
            // referring to sslSocketFactory(SSLSocketFactory, X509TrustManager) docs:
            /**
             * Most applications should not call this method, and instead use the system defaults.
             * Those classes include special optimizations that can be lost
             * if the implementations are decorated.
             */
            sslSocketFactory(sslContext.socketFactory, trustManager)
            addInterceptor(ChangellyInterceptor())
            addInterceptor(DigitalSignatureInterceptor(userKeyPair))
            if (!BuildConfig.DEBUG) return@apply
            addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        }.build()
    }

    private fun getChangellyHttpClient(): OkHttpClient {
        return OkHttpClient.Builder().apply {
            connectTimeout(15, TimeUnit.SECONDS)
            readTimeout(15, TimeUnit.SECONDS)
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
            .baseUrl(ExchangeKeys.EXCHANGE_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(getChangellyHttpClient())
            .build()
            .create(ChangellyAPIService::class.java)
}
