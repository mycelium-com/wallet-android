package com.mycelium.wallet.external.mediaflow;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

public class NewsFactory {
    private static final String ENDPOINT = "https://public-api.wordpress.com/rest/v1.1/sites/blog.mycelium.com/";
    public static ObjectMapper objectMapper = new ObjectMapper() {
        {
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);
        }
    };

    public static NewsApiService getService() {
        return new Retrofit.Builder()
                .baseUrl(ENDPOINT)
                .addConverterFactory(JacksonConverterFactory.create(objectMapper))
                .build()
                .create(NewsApiService.class);
    }
}
