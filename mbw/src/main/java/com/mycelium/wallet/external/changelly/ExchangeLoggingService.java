package com.mycelium.wallet.external.changelly;


import com.mycelium.wallet.BuildConfig;
import com.mycelium.wallet.external.changelly.model.Order;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ExchangeLoggingService {
    String endPoint = BuildConfig.FLAVOR.equals("btctestnet")
            ? "https://wallet-exchange-admin-stg.mycelium.com/api/"
            : "https://wallet-exchange-admin.mycelium.com/api/";

    @POST("orders")
    Call<Void> saveOrder(@Body Order order);

    ExchangeLoggingService exchangeLoggingService = new Retrofit.Builder()
            .baseUrl(endPoint)
            .addConverterFactory(JacksonConverterFactory.create())
            .client(new OkHttpClient.Builder().build())
            .build().create(ExchangeLoggingService.class);
}
