package com.mycelium.wallet.activity.rmc;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.mycelium.wallet.activity.rmc.json.BtcPoolResponse;

import java.io.IOException;

public class BtcPoolStatisticsManager {

    public static String ACCOUNT_INFO_API_URL = "https://bitcoin-russia.ru/api/accounts/13Zhk1JbpUmWfSMcyzLTAv7A6VkuecvaJb";

    class PoolStatisticInfo {
        double totalRmcHashrate;
        double yourRmcHashrate;

        public PoolStatisticInfo(double totalRmcHashrate, double yourRmcHashrate) {
            this.totalRmcHashrate = totalRmcHashrate;
            this.yourRmcHashrate = yourRmcHashrate;
        }
    }

    public PoolStatisticInfo getStatistics() {
        BtcPoolResponse.Json response = getInfo();
        return new PoolStatisticInfo(response.hashrate / 3600, 0);
    }

    private BtcPoolResponse.Json getInfo() {
        HttpRequestFactory requestFactory = new NetHttpTransport()
                .createRequestFactory(new HttpRequestInitializer() {
                    @Override
                    public void initialize(HttpRequest request) {
                        request.setParser(new JsonObjectParser(new JacksonFactory()));
                    }
                });
        try {
            HttpRequest request = requestFactory.buildGetRequest(new GenericUrl(ACCOUNT_INFO_API_URL));
            HttpResponse response = request.execute();
            return response.parseAs(BtcPoolResponse.Json.class);
        } catch (IOException ex) {
        }
        return null;
    }
}
