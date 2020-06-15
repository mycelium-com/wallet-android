package com.mycelium.bequant.remote

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.mycelium.bequant.BequantPreference
import com.mycelium.bequant.remote.model.KYCApplicant
import com.mycelium.bequant.remote.model.KYCCreateRequest
import com.mycelium.bequant.remote.model.KYCDocument
import com.mycelium.bequant.remote.model.ProgressRequestBody
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.io.File


class KYCRepository {
    fun create(scope: CoroutineScope, applicant: KYCApplicant, success: (() -> Unit)) {
        scope.launch(Dispatchers.IO) {
            val result = service.create(KYCCreateRequest(applicant))
            if (result.isSuccessful) {
                uuid = result.body()?.uuid ?: ""
                withContext(Dispatchers.Main) {
                    success.invoke()
                }
            }
        }
    }

    fun mobileVerification(scope: CoroutineScope, success: ((String) -> Unit)) {
        scope.launch(Dispatchers.IO) {
            val result = service.mobileVerification(uuid)
            if (result.isSuccessful) {
                withContext(Dispatchers.Main) {
                    success.invoke(result.body()?.message ?: "")
                }
            }
        }
    }

    fun checkMobileVerification(scope: CoroutineScope, code: String,
                                success: (() -> Unit), error: (() -> Unit)) {
        scope.launch(Dispatchers.IO) {
            val result = service.checkMobileVerification(uuid, code)
            if (result.isSuccessful) {
                withContext(Dispatchers.Main) {
                    if (result.body()?.message == "CODE_VALID") {
                        success.invoke()
                    } else {
                        error.invoke()
                    }
                }
            }
        }
    }

    fun uploadDocument(scope: CoroutineScope, type: KYCDocument, file: File,
                       progress: ((Long, Long) -> Unit), success: (() -> Unit)) {
        scope.launch(Dispatchers.IO) {
            val fileBody = ProgressRequestBody(file, "image")
            fileBody.progressListener = progress
            val multipartBody = MultipartBody.Part.createFormData("file", file.name, fileBody)
            val result = service.uploadFile(uuid, type, "ITA", multipartBody)
            if (result.isSuccessful) {
                withContext(Dispatchers.Main) {
                    success.invoke()
                }
            }
        }
    }

    companion object {
        val ENDPOINT = "https://test006.bqtstuff.com/"

        var uuid: String = ""


        private val objectMapper = ObjectMapper()
                .registerKotlinModule()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true)
                .configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true)

        val repository by lazy { KYCRepository() }

        val service by lazy {
            Retrofit.Builder()
                    .baseUrl(ENDPOINT)
                    .client(OkHttpClient.Builder()
                            .addInterceptor {
                                it.proceed(it.request().newBuilder().apply {
                                    header("Content-Type", "application/json")
                                    if (BequantPreference.getAccessToken().isNotEmpty()) {
                                        header("x-access-token", "xyz")
                                    }
                                }.build())
                            }
                            .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                            .build())
                    .addConverterFactory(NullOnEmptyConverterFactory())
                    .addConverterFactory(JacksonConverterFactory.create(objectMapper))
                    .build()
                    .create(BequantKYCApiService::class.java)
        }
    }
}