package com.mycelium.wallet.external.news;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycelium.wallet.api.retrofit.JacksonConverter;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;

public class NewsFactory {
    private static final String ENDPOINT = "https://public-api.wordpress.com/rest/v1.1/sites/blog.mycelium.com";
    public static ObjectMapper objectMapper = new ObjectMapper() {
        {
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);
        }
    };

    public static NewsService getService() {
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(ENDPOINT)
                .setConverter(new JacksonConverter(objectMapper))
                .setRequestInterceptor(new RequestInterceptor() {
                    @Override
                    public void intercept(RequestFacade request) {
                        request.addHeader("Content-Type", "application/json");
                    }
                })
                .build();
        return restAdapter.create(NewsService.class);
    }
}
