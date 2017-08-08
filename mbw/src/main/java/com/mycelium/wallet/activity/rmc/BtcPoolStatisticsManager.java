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
import com.mycelium.wallet.colu.ColuAccount;

import java.io.IOException;
import java.math.BigDecimal;

public class BtcPoolStatisticsManager {

    public static String ACCOUNT_INFO_API_URL = "https://bitcoin-russia.ru/api/accounts/13Zhk1JbpUmWfSMcyzLTAv7A6VkuecvaJb";
    public static int TOTAL_RMC_COUNT = 250000;

    private ColuAccount coluAccount;

    class PoolStatisticInfo {
        double totalRmcHashrate;
        double yourRmcHashrate;

        public PoolStatisticInfo(double totalRmcHashrate, double yourRmcHashrate) {
            this.totalRmcHashrate = totalRmcHashrate;
            this.yourRmcHashrate = yourRmcHashrate;
        }
    }

    public BtcPoolStatisticsManager(ColuAccount coluAccount) {
        this.coluAccount = coluAccount;
    }

    public PoolStatisticInfo getStatistics() {
        BigDecimal rmcBalance = coluAccount.getCurrencyBasedBalance().confirmed.getValue();
        BtcPoolResponse.Json response = getInfo();

        if (response == null)
            return null;

        double totalRmcHashrate = response.hashrate / 3600;
        double yourRmcHashrate = totalRmcHashrate * rmcBalance.doubleValue() / TOTAL_RMC_COUNT;
        return new PoolStatisticInfo(totalRmcHashrate, yourRmcHashrate);
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
