package com.mycelium.wallet.external.rmc.remote;

import java.util.List;
import java.util.Map;

import retrofit.http.GET;
import retrofit.http.Path;

public interface StatRmcService {
    @GET("/stats/hashrate")
    long getCommonHashrate();

    @GET("/hashrate/{address}")
    long getHashrate(@Path("address") String address);

    @GET("/balance/{address}")
    long getBalance(@Path("address") String address);

    @GET("/payments/{address}")
    Map<String, List<String>> getPaidTransactions(@Path("address") String address);
}
