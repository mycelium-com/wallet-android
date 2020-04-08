package com.mycelium.bequant.remote

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.mycelium.bequant.remote.model.Auth
import com.mycelium.bequant.remote.model.Register
import com.mycelium.bequant.remote.model.RegisterResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory


class SignRepository {
    fun authorize(auth: Auth) {
        service.authorize(auth).enqueue(object : Callback<Auth> {
            override fun onFailure(call: Call<Auth>, t: Throwable) {
            }

            override fun onResponse(call: Call<Auth>, response: Response<Auth>) {
            }
        })
    }

    fun register(register: Register, success: () -> Unit, error: () -> Unit) {
        service.register(register).enqueue(object : Callback<RegisterResponse> {
            override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                error.invoke()
            }

            override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
                if (response.isSuccessful) {
                    success.invoke()
                } else {
                    error.invoke()
                }
            }
        })
    }

    companion object {
        val ENDPOINT = "http://144.76.140.152:8111/"

        private val objectMapper = ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true)

        val repository by lazy { SignRepository() }


        val service by lazy {
            Retrofit.Builder()
                    .baseUrl(ENDPOINT)
                    .client(OkHttpClient.Builder()
                            .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                            .build())
                    .addConverterFactory(JacksonConverterFactory.create(objectMapper))
                    .build()
                    .create(BequantService::class.java)
        }
    }
}