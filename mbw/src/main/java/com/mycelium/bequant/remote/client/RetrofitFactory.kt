package com.mycelium.bequant.remote.client

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.mycelium.bequant.BequantPreference
import com.mycelium.bequant.Constants
import com.mycelium.bequant.remote.NullOnEmptyConverterFactory
import com.mycelium.wallet.BuildConfig
import okhttp3.Call
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.text.SimpleDateFormat
import java.util.Locale

object RetrofitFactory {
    val objectMapper: ObjectMapper = ObjectMapper()
            .registerKotlinModule()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true)
            .configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true)
            .setDateFormat(SimpleDateFormat("yyyy-MM-dd", Locale.US))

    private fun getClientBuilder(withAccessToken: Boolean = false): OkHttpClient.Builder =
            OkHttpClient().newBuilder()
                    .addInterceptor {
                        it.proceed(it.request().newBuilder().apply {
                            header("Content-Type", "application/json")
                            if (withAccessToken) {
                                header("Authorization", "Bearer ${BequantPreference.getAccessToken()}")
                            } else {
                                header("Authorization",
                                        Credentials.basic(BequantPreference.getPublicKey(),
                                                BequantPreference.getPrivateKey()))
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


    private fun getBuilder(url: String, withAccessToken: Boolean = false): Retrofit.Builder =
            Retrofit.Builder()
                    .callFactory(object : Call.Factory {
                        //create client lazy on demand in background thread
                        //see https://www.zacsweers.dev/dagger-party-tricks-deferred-okhttp-init/
                        private val client by lazy { getClientBuilder(withAccessToken).build() }

                        override fun newCall(request: Request): Call = client.newCall(request)
                    })
                    .baseUrl(url)
                    .addConverterFactory(NullOnEmptyConverterFactory())
                    .addConverterFactory(JacksonConverterFactory.create(objectMapper))


    fun getRetrofit(url: String, withAccessToken: Boolean = false): Retrofit =
            getBuilder(url, withAccessToken)
                    .addConverterFactory(NullOnEmptyConverterFactory())
                    .addConverterFactory(JacksonConverterFactory.create(objectMapper))
                    .build()

}

inline fun <reified T> createApi(url: String = Constants.ACCOUNT_ENDPOINT_POSTFIX, withAccessToken: Boolean = false): T =
        RetrofitFactory.getRetrofit(url, withAccessToken).create(T::class.java)
