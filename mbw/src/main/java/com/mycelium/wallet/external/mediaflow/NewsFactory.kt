package com.mycelium.wallet.external.mediaflow

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper

import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory

object NewsFactory {
    private const val ENDPOINT = "https://public-api.wordpress.com/rest/v1.2/sites/media.mycelium.com/"
    var objectMapper: ObjectMapper = ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true)

    val service: NewsApiService
        get() = Retrofit.Builder()
                .baseUrl(ENDPOINT)
                .addConverterFactory(JacksonConverterFactory.create(objectMapper))
                .build()
                .create(NewsApiService::class.java)
}
