package com.mycelium.wallet.activity.rmc;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.IOUtils;
import com.mycelium.wallet.colu.ColuAccount;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;

public class BtcPoolStatisticsManager {

    public static String HASHRATE_INFO_API_URL = "http://188.65.212.157/api/stats/hashrate";

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
        Long hashRate = getHashRate();

        if (hashRate == null)
            return null;

        double totalRmcHashrate = hashRate;
        double yourRmcHashrate = totalRmcHashrate * rmcBalance.doubleValue() / Keys.TOTAL_RMC_COUNT;
        return new PoolStatisticInfo(totalRmcHashrate, yourRmcHashrate);
    }

    private Long getHashRate() {
        HttpRequestFactory requestFactory = new NetHttpTransport()
                .createRequestFactory(new HttpRequestInitializer() {
                    @Override
                    public void initialize(HttpRequest request) {
                        request.setParser(new JsonObjectParser(new JacksonFactory()));
                    }
                });
        try {
            HttpRequest request = requestFactory.buildGetRequest(new GenericUrl(HASHRATE_INFO_API_URL));
            HttpResponse response = request.execute();
            InputStream inputStream = response.getContent();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IOUtils.copy(inputStream, baos, true);
            return Long.parseLong(baos.toString().replace("\n", ""));
        } catch (IOException ex) {
        }
        return null;
    }
}
