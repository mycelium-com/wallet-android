package com.mycelium.bequant.remote.client

import com.mycelium.bequant.BequantPreference
import com.mycelium.bequant.Constants
import com.mycelium.bequant.remote.NullOnEmptyConverterFactory
import com.mycelium.bequant.remote.repositories.KYCRepository
import com.mycelium.wallet.BuildConfig
import okhttp3.Call
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory

object RetrofitFactory {

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
                    .addConverterFactory(JacksonConverterFactory.create(KYCRepository.objectMapper))


    fun getRetrofit(url: String, withAccessToken: Boolean = false): Retrofit =
            getBuilder(url, withAccessToken)
                    .addConverterFactory(NullOnEmptyConverterFactory())
                    .addConverterFactory(JacksonConverterFactory.create(KYCRepository.objectMapper))
                    .build()

}

inline fun <reified T> createApi(url: String = Constants.ACCOUNT_ENDPOINT_POSTFIX, withAccessToken: Boolean = false): T =
        RetrofitFactory.getRetrofit(url, withAccessToken).create(T::class.java)
