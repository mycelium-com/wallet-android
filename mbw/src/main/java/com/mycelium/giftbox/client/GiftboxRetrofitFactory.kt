package com.mycelium.giftbox.client

import android.os.Build
import android.util.Log
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.mycelium.bequant.remote.NullOnEmptyConverterFactory
import com.mycelium.giftbox.client.GiftboxConstants.MC_API_KEY
import com.mycelium.wallet.BuildConfig
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import okio.Buffer
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

interface SignatureProvider {
    fun address(): String
    fun signature(data: String): String
}

object RetrofitFactory {
    val objectMapper: ObjectMapper = ObjectMapper()
        .registerKotlinModule()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true)
        .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
        .configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true)
        .setDateFormat(SimpleDateFormat("yyyy-MM-dd", Locale.US))

    private fun getClientBuilder(signatureProvider: SignatureProvider? = null): OkHttpClient.Builder {
        val sslContext = SSLContext.getInstance("TLS")

        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(null as KeyStore?)
        val trustManagers = trustManagerFactory.trustManagers
        val trustManager = trustManagers.first { it is X509TrustManager } as X509TrustManager

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            sslContext.init(null, arrayOf(trustManager), null)
        } else {
            val trustAllCerts = arrayOf<TrustManager>(
                object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                }
            )
            sslContext.init(null, trustAllCerts, null)
        }

        return OkHttpClient().newBuilder()
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .addInterceptor {
                it.proceed(it.request().newBuilder().apply {
                    addHeader("Content-Type", "application/json")
                    addHeader("Accept-Language", Locale.getDefault().language)
                    addHeader("x-api-key", MC_API_KEY)
//                    addHeader("Authorization", "Basic ${GiftboxPreference.getAccessToken()}")
                    signatureProvider?.run {
                        addHeader("wallet-address", signatureProvider.address())
                        val body = try {
                            Buffer().apply {
                                it.request().body?.writeTo(this)
                            }.readUtf8()
                        } catch (e: Exception) {
                            Log.e("Giftbox", "Error getting signature", e)
                            ""
                        }
                        addHeader("wallet-signature", signatureProvider.signature(body))
                    }
                }.build())
            }
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    })
                }
            }
    }


    fun getRetrofit(url: String, signatureProvider: SignatureProvider? = null): Retrofit =
        Retrofit.Builder()
            .callFactory(object : Call.Factory {
                //create client lazy on demand in background thread
                //see https://www.zacsweers.dev/dagger-party-tricks-deferred-okhttp-init/
                private val client by lazy {
                    getClientBuilder(signatureProvider)
                        .connectTimeout(15, TimeUnit.SECONDS)
                        .readTimeout(15, TimeUnit.SECONDS)
                        .build()
                }

                override fun newCall(request: Request): Call = client.newCall(request)
            })
            .baseUrl(url)
            .addConverterFactory(NullOnEmptyConverterFactory())
            .addConverterFactory(JacksonConverterFactory.create(objectMapper))
            .build()

}

inline fun <reified T> createApi(
    url: String = GiftboxConstants.ENDPOINT,
    signatureProvider: SignatureProvider? = null
): T =
    RetrofitFactory.getRetrofit(url, signatureProvider).create(T::class.java)
