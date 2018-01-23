package com.mycelium.wallet.exchange;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycelium.wallet.api.retrofit.JacksonConverter;
import com.mycelium.wallet.exchange.model.CoinmarketcapRate;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;

public class CoinmarketcapApi {
    private static final String RMC_API_RATE = "https://api.coinmarketcap.com";

    public static CoinmarketcapRate getRate() {
        ObjectMapper objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true)
                .configure(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS, true);

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(RMC_API_RATE)
                .setConverter(new JacksonConverter(objectMapper))
                .setRequestInterceptor(new RequestInterceptor() {
                    @Override
                    public void intercept(RequestFacade request) {
                        request.addHeader("Content-Type", "application/json");
                    }
                })
                .build();
        return restAdapter.create(CoinmarketcapService.class).getRmcRate();
    }
}
