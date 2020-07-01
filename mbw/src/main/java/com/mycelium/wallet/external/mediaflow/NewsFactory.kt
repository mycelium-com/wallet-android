package com.mycelium.wallet.external.mediaflow

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.mycelium.wallet.BuildConfig

import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory

object NewsFactory {
    private val objectMapper: ObjectMapper = ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true)

    val service by lazy {
        Retrofit.Builder()
                .baseUrl(BuildConfig.MEDIA_FLOW_URL)
                .addConverterFactory(JacksonConverterFactory.create(objectMapper))
                .build()
                .create(NewsApiService::class.java)
    }
}
