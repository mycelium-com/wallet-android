package com.mycelium.giftbox.client

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
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

object RetrofitFactory {
    val objectMapper: ObjectMapper = ObjectMapper()
        .registerKotlinModule()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true)
        .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
        .configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true)
        .setDateFormat(SimpleDateFormat("yyyy-MM-dd", Locale.US))

    private fun getClientBuilder(): OkHttpClient.Builder =
        OkHttpClient().newBuilder()
            .addInterceptor {
                it.proceed(it.request().newBuilder().apply {
//                    addHeader("Content-Type", "application/json")
                    addHeader("x-api-key", MC_API_KEY)
//                    addHeader("Authorization", "Basic ${GiftboxPreference.getAccessToken()}")
                }.build())
            }
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    })
                }
            }


    fun getRetrofit(url: String): Retrofit =
        Retrofit.Builder()
            .callFactory(object : Call.Factory {
                //create client lazy on demand in background thread
                //see https://www.zacsweers.dev/dagger-party-tricks-deferred-okhttp-init/
                private val client by lazy { getClientBuilder().build() }

                override fun newCall(request: Request): Call = client.newCall(request)
            })
            .baseUrl(url)
            .addConverterFactory(NullOnEmptyConverterFactory())
            .addConverterFactory(JacksonConverterFactory.create(objectMapper))
            .build()

}

inline fun <reified T> createApi(url: String = GiftboxConstants.ENDPOINT): T =
    RetrofitFactory.getRetrofit(url).create(T::class.java)
