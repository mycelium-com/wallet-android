package com.mycelium.bequant.remote

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.mycelium.bequant.BequantPreference
import com.mycelium.bequant.remote.model.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory


class SignRepository {
    fun authorize(auth: Auth, success: () -> Unit, error: (String) -> Unit) {
        service.authorize(auth).enqueue(object : Callback<AuthResponse> {
            override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                error.invoke(t.message ?: "")
            }

            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                if (response.isSuccessful) {
                    BequantPreference.setSession(response.body()?.session ?: "")
                    success.invoke()
                } else {
                    error.invoke(response.message())
                }
            }
        })
    }

    fun register(register: Register, success: () -> Unit, error: (String) -> Unit) {
        service.register(register).enqueue(object : Callback<Void> {
            override fun onFailure(call: Call<Void>, t: Throwable) {
                error.invoke(t.message ?: "")
                t.printStackTrace()
            }

            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    success.invoke()
                } else {
                    error.invoke(response.message())
                }
            }
        })
    }

    fun resendRegister(email: Email, success: () -> Unit, error: (String) -> Unit) {
        service.resendRegister(email).enqueue(object : Callback<Void> {
            override fun onFailure(call: Call<Void>, t: Throwable) {
                error.invoke(t.message ?: "")
                t.printStackTrace()
            }

            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    success.invoke()
                } else {
                    error.invoke(response.message())
                }
            }
        })
    }

    fun confirmEmail(confirmToken: String, success: () -> Unit, error: (String) -> Unit) {
        service.emailConfirm(confirmToken).enqueue(object : Callback<BequantResponse?> {
            override fun onFailure(call: Call<BequantResponse?>, t: Throwable) {
                error.invoke(t.message ?: "")
            }

            override fun onResponse(call: Call<BequantResponse?>, response: Response<BequantResponse?>) {
                if (response.isSuccessful) {
                    success.invoke()
                } else {
                    error.invoke(response.message())
                }
            }
        })
    }


    fun totpList(success: () -> Unit, error: (String) -> Unit) {
        service.totpList().enqueue(object : Callback<TotpListResponse> {
            override fun onFailure(call: Call<TotpListResponse>, t: Throwable) {
                error.invoke(t.message ?: "")
            }

            override fun onResponse(call: Call<TotpListResponse>, response: Response<TotpListResponse>) {
                if (response.isSuccessful) {
                    success.invoke()
                } else {
                    error.invoke(response.message())
                }
            }
        })
    }

    fun totpConfirm(success: () -> Unit, error: (String) -> Unit) {
        service.totpConfirm(BequantPreference.getSession()).enqueue(object : Callback<TotpConfirmResponse> {
            override fun onFailure(call: Call<TotpConfirmResponse>, t: Throwable) {
                error.invoke(t.message ?: "")
            }

            override fun onResponse(call: Call<TotpConfirmResponse>, response: Response<TotpConfirmResponse>) {
                if (response.isSuccessful) {
                    success.invoke()
                } else {
                    error.invoke(response.message())
                }
            }
        })
    }

    fun totpCreate(success: (Int, String, String) -> Unit, error: (String) -> Unit) {
        service.totpCreate().enqueue(object : Callback<TotpCreateResponse?> {
            override fun onFailure(call: Call<TotpCreateResponse?>, t: Throwable) {
                error.invoke(t.message ?: "")
            }

            override fun onResponse(call: Call<TotpCreateResponse?>, response: Response<TotpCreateResponse?>) {
                if (response.isSuccessful) {
                    success.invoke(response.body()?.otpId!!, response.body()?.otpLink!!, response.body()?.backupPassword!!)
                } else {
                    error.invoke(response.message())
                }
            }
        })
    }

    fun totpActivate(otpId: Int, passcode: String, success: () -> Unit, error: (String) -> Unit) {
        service.totpActivate(TotpActivate(otpId, passcode)).enqueue(object : Callback<BequantResponse?> {
            override fun onFailure(call: Call<BequantResponse?>, t: Throwable) {
                error.invoke(t.message ?: "")
            }

            override fun onResponse(call: Call<BequantResponse?>, response: Response<BequantResponse?>) {
                if (response.isSuccessful) {
                    success.invoke()
                } else {
                    error.invoke(response.message())
                }
            }
        })
    }

    fun resetPassword(email: String, success: () -> Unit, error: (String) -> Unit) {
        service.resetPassword(Email(email)).enqueue(object : Callback<RegisterResponse> {
            override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                error.invoke(t.message ?: "")
            }

            override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
                if (response.isSuccessful) {
                    success.invoke()
                } else {
                    error.invoke(response.message())
                }
            }
        })
    }

    fun resetPasswordUpdate(passwordSet: PasswordSet, success: () -> Unit, error: (String) -> Unit) {
        service.resetPasswordSet(passwordSet).enqueue(object : Callback<RegisterResponse> {
            override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                error.invoke(t.message ?: "")
            }

            override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
                if (response.isSuccessful) {
                    success.invoke()
                } else {
                    error.invoke(response.message())
                }
            }
        })
    }

    companion object {
        val ENDPOINT = "https://xwpe71x4sg.execute-api.us-east-1.amazonaws.com/prd-reg/"

        private val objectMapper = ObjectMapper()
                .registerKotlinModule()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true)
                .configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true);

        val repository by lazy { SignRepository() }


        val service by lazy {
            Retrofit.Builder()
                    .baseUrl(ENDPOINT)
                    .client(OkHttpClient.Builder()
                            .addInterceptor {
                                it.proceed(it.request().newBuilder().apply {
                                    header("Content-Type", "application/json")
                                    if (BequantPreference.getSession().isNotEmpty()) {
                                        header("Authorization", "Bearer ${BequantPreference.getSession()}")
                                    }
                                }.build())
                            }
                            .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                            .build())
                    .addConverterFactory(JacksonConverterFactory.create(objectMapper))
                    .build()
                    .create(BequantService::class.java)
        }
    }
}