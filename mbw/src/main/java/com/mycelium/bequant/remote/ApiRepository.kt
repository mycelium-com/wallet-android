package com.mycelium.bequant.remote

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.mycelium.bequant.BequantPreference
import com.mycelium.bequant.remote.model.BequantBalance
import com.mycelium.bequant.remote.model.Currency
import com.mycelium.bequant.remote.model.DepositAddress
import com.mycelium.bequant.remote.model.Ticker
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory


class ApiRepository {

    fun balances(success: (List<BequantBalance>) -> Unit, error: (Int, String) -> Unit) {
        service.balance().enqueue(object : Callback<List<BequantBalance>> {
            override fun onFailure(call: Call<List<BequantBalance>>, t: Throwable) {
                error.invoke(0, t.message ?: "")
            }

            override fun onResponse(call: Call<List<BequantBalance>>, response: Response<List<BequantBalance>>) {
                if (response.isSuccessful) {
                    success.invoke(response.body() ?: listOf())
                } else {
                    error.invoke(response.code(), response.errorBody()?.string() ?: "")
                }
            }
        })
    }

    fun tickers(success: (List<Ticker>) -> Unit, error: (Int, String) -> Unit) {
        service.tickers().enqueue(object : Callback<List<Ticker>> {
            override fun onFailure(call: Call<List<Ticker>>, t: Throwable) {
                error.invoke(0, t.message ?: "")
            }

            override fun onResponse(call: Call<List<Ticker>>, response: Response<List<Ticker>>) {
                if (response.isSuccessful) {
                    success.invoke(response.body() ?: listOf())
                } else {
                    error.invoke(response.code(), response.errorBody()?.string() ?: "")
                }
            }
        })
    }

    fun depositAddress(currency: String, success: (DepositAddress) -> Unit, error: (Int, String) -> Unit) {
        service.depositAddress(adopt(currency)).enqueue(object : Callback<DepositAddress> {
            override fun onFailure(call: Call<DepositAddress>, t: Throwable) {
                error.invoke(0, t.message ?: "")
            }

            override fun onResponse(call: Call<DepositAddress>, response: Response<DepositAddress>) {
                if (response.isSuccessful) {
                    success.invoke(response.body()!!)
                } else {
                    error.invoke(response.code(), response.errorBody()?.string() ?: "")
                }
            }
        })
    }

    fun createDepositAddress(currency: String, success: (DepositAddress) -> Unit, error: (Int, String) -> Unit) {
        service.createDepositAddress(adopt(currency)).enqueue(object : Callback<DepositAddress> {
            override fun onFailure(call: Call<DepositAddress>, t: Throwable) {
                error.invoke(0, t.message ?: "")
            }

            override fun onResponse(call: Call<DepositAddress>, response: Response<DepositAddress>) {
                if (response.isSuccessful) {
                    success.invoke(response.body()!!)
                } else {
                    error.invoke(response.code(), response.errorBody()?.string() ?: "")
                }
            }
        })
    }

    fun currencies(success: (List<Currency>) -> Unit, error: (Int, String) -> Unit) {
        service.currencies().enqueue(object : Callback<List<Currency>> {
            override fun onFailure(call: Call<List<Currency>>, t: Throwable) {
                error.invoke(0, t.message ?: "")
            }

            override fun onResponse(call: Call<List<Currency>>, response: Response<List<Currency>>) {
                if (response.isSuccessful) {
                    success.invoke(response.body()!!)
                } else {
                    error.invoke(response.code(), response.errorBody()?.string() ?: "")
                }
            }
        })
    }

    companion object {
        fun adopt(currency: String) = if (currency.startsWith("t")) currency.substring(1) else currency

        val ENDPOINT = "https://fynh6mvro0.execute-api.us-east-1.amazonaws.com/prd/"

        private val objectMapper = ObjectMapper()
                .registerKotlinModule()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true)
                .configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true)

        val repository by lazy { ApiRepository() }


        val service by lazy {
            Retrofit.Builder()
                    .baseUrl(ENDPOINT)
                    .client(OkHttpClient.Builder()
                            .addInterceptor {
                                it.proceed(it.request().newBuilder().apply {
                                    header("Content-Type", "application/json")
                                    header("Authorization",
                                            Credentials.basic(BequantPreference.getPublicKey(),
                                                    BequantPreference.getPrivateKey()))
                                }.build())
                            }
                            .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                            .build())
                    .addConverterFactory(JacksonConverterFactory.create(objectMapper))
                    .build()
                    .create(BequantApiService::class.java)
        }
    }
}