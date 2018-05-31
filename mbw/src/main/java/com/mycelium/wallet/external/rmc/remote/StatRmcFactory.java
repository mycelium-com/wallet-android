package com.mycelium.wallet.external.rmc.remote;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycelium.wallet.api.retrofit.JacksonConverter;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;

public class StatRmcFactory {
    private static final String STAT_RMC_ENDPOINT = "https://stat.rmc.one/api";

    public static StatRmcService getService() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(STAT_RMC_ENDPOINT)
                .setConverter(new JacksonConverter(objectMapper))
                .setRequestInterceptor(new RequestInterceptor() {
                    @Override
                    public void intercept(RequestFacade request) {
                        request.addHeader("Content-Type", "application/json");
                    }
                })
                .build();
        return restAdapter.create(StatRmcService.class);
    }
}
