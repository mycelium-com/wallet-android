package com.mycelium.wallet.activity.rmc;

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.IOUtils;
import com.mrd.bitlib.model.Address;
import com.mycelium.wallet.activity.rmc.model.BitcoinNetworkStats;
import com.mycelium.wallet.colu.ColuAccount;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class BtcPoolStatisticsManager {

    public static String HASHRATE_INFO_API_URL = "https://stat.rmc.one/api/stats/hashrate";
    public static String YOUR_RMC_HASHRATE_INFO_URL = "https://stat.rmc.one/api/hashrate/";
    public static String BITCOIN_NETWORK_STATS_URL = "https://api.blockchain.info/stats";

    private ColuAccount coluAccount;

    public static class PoolStatisticInfo {
        public long totalRmcHashrate;
        public long yourRmcHashrate;
        public long difficulty;

        public PoolStatisticInfo(long totalRmcHashrate, long yourRmcHashrate) {
            this.totalRmcHashrate = totalRmcHashrate;
            this.yourRmcHashrate = yourRmcHashrate;
        }
    }

    public BtcPoolStatisticsManager(ColuAccount coluAccount) {
        this.coluAccount = coluAccount;
    }

    public PoolStatisticInfo getStatistics() {
        Long totalRmcHashrate = getHashRate();
        if (totalRmcHashrate == null) {
            totalRmcHashrate = 0L;
        }

        Long yourRmcHashrate = getYourRmcHahrate(coluAccount.getAddress());
        if (yourRmcHashrate == null) {
            yourRmcHashrate = 0L;
        }
        PoolStatisticInfo info = new PoolStatisticInfo(totalRmcHashrate, yourRmcHashrate);
        BitcoinNetworkStats stats = getBitcoinNetworkStats();
        if(stats != null) {
            info.difficulty = stats.difficulty;
        }
        return info;
    }

    private BitcoinNetworkStats getBitcoinNetworkStats() {
        HttpRequestFactory requestFactory = new NetHttpTransport()
                .createRequestFactory(new HttpRequestInitializer() {
                    @Override
                    public void initialize(HttpRequest request) {
                    }
                });
        try {
            HttpRequest request = requestFactory.buildGetRequest(new GenericUrl(BITCOIN_NETWORK_STATS_URL));
            HttpResponse response = request.execute();
            InputStream inputStream = response.getContent();
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return objectMapper.readValue(inputStream, BitcoinNetworkStats.class);
        } catch (Exception e) {
            Log.e("Btc Stats", "", e);
        }
        return null;
    }

    private Long getYourRmcHahrate(Address address) {
        HttpRequestFactory requestFactory = new NetHttpTransport()
                .createRequestFactory(new HttpRequestInitializer() {
                    @Override
                    public void initialize(HttpRequest request) {
                        request.setParser(new JsonObjectParser(new JacksonFactory()));
                    }
                });
        try {
            HttpRequest request = requestFactory.buildGetRequest(new GenericUrl(YOUR_RMC_HASHRATE_INFO_URL + address.toString()));
            HttpResponse response = request.execute();
            InputStream inputStream = response.getContent();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IOUtils.copy(inputStream, baos, true);
            return Long.parseLong(baos.toString().replace("\n", ""));
        } catch (Exception ignored) {
        }
        return null;
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
        } catch (Exception ignored) {
        }
        return null;
    }
}
