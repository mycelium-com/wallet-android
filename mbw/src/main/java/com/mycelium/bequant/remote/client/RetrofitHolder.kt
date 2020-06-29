package com.mycelium.bequant.remote.client

import com.mycelium.bequant.BequantPreference
import com.mycelium.bequant.Constants
import com.mycelium.wallet.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Call
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.*

object RetrofitHolder {

    val clientBuilder: OkHttpClient.Builder by lazy {
        OkHttpClient().newBuilder()
                .addInterceptor {
                    it.proceed(it.request().newBuilder().apply {
                        header("Content-Type", "application/json")
                        header("Authorization",
                                Credentials.basic(BequantPreference.getPublicKey(),
                                        BequantPreference.getPrivateKey()))
                    }.build())
                }
//                .addInterceptor(HeaderParamInterceptor("BearerAuth", "Authorization", this::authorizationGenerator))
                .apply {
                    if (BuildConfig.DEBUG) {
                        addInterceptor(HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BODY
                        })
                    }
                }
    }

    private fun getBuilder(url: String): Retrofit.Builder {
        val moshi = Moshi.Builder()
                .add(EnumJsonAdapterFactory)
                .add(KotlinJsonAdapterFactory())
                .add(Date::class.java, Rfc3339DateJsonAdapter().nullSafe())
                .build()

        return Retrofit.Builder()
                .callFactory(object : Call.Factory {
                    //create client lazy on demand in background thread
                    //see https://www.zacsweers.dev/dagger-party-tricks-deferred-okhttp-init/
                    private val client by lazy { clientBuilder.build() }
                    override fun newCall(request: Request): Call = client.newCall(request)
                })
                .baseUrl(url)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(EnumRetrofitConverterFactory)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
    }

    fun getRetrofit(url: String): Retrofit {
        val moshi = Moshi.Builder()
                .add(EnumJsonAdapterFactory)
                .add(KotlinJsonAdapterFactory())
                .build()
        return getBuilder(url)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(EnumRetrofitConverterFactory)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
    }
}