package com.mycelium.wallet.activity.rmc;

import android.util.Log;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.mycelium.wallet.activity.rmc.model.BitcoinNetworkStats;
import com.mycelium.wallet.colu.ColuAccount;
import com.mycelium.wallet.external.rmc.remote.StatRmcFactory;
import com.mycelium.wallet.external.rmc.remote.StatRmcService;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class BtcPoolStatisticsManager {

    public static final String TAG = "RMCStatistic";
    public static String BITCOIN_NETWORK_STATS_URL = "https://api.blockchain.info/stats";

    private ColuAccount coluAccount;

    public static class PoolStatisticInfo {
        public long totalRmcHashrate;
        public long yourRmcHashrate;
        public long difficulty;
        public long accruedIncome;

        public PoolStatisticInfo(long totalRmcHashrate, long yourRmcHashrate) {
            this.totalRmcHashrate = totalRmcHashrate;
            this.yourRmcHashrate = yourRmcHashrate;
        }
    }

    public BtcPoolStatisticsManager(ColuAccount coluAccount) {
        this.coluAccount = coluAccount;
    }

    public PoolStatisticInfo getStatistics() {
        StatRmcService service = StatRmcFactory.getService();
        long totalRmcHashrate = 0;
        try {
            totalRmcHashrate = service.getCommonHashrate();
        } catch (Exception e) {
            Log.e(TAG, "service.getCommonHashrate", e);
        }

        String address = coluAccount.getAddress().toString();
        long yourRmcHashrate = 0;
        try {
            yourRmcHashrate = service.getHashrate(address);
        } catch (Exception e) {
            Log.e(TAG, "service.getHashrate", e);
        }

        long accruedIncome = 0;
        try {
            accruedIncome = service.getBalance(address);
        } catch (Exception e) {
            Log.e(TAG, "service.getBalance", e);
        }
        try {
            Map<String, List<String>> paidTransactions = service.getPaidTransactions(address);
            if (paidTransactions != null) {
                for (List<String> thx : paidTransactions.values()) {
                    accruedIncome += Long.parseLong(thx.get(0));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "service.getPaidTransactions", e);
        }
        PoolStatisticInfo info = new PoolStatisticInfo(totalRmcHashrate, yourRmcHashrate);
        BitcoinNetworkStats stats = getBitcoinNetworkStats();
        if (stats != null) {
            info.difficulty = stats.difficulty;
        }
        info.accruedIncome = accruedIncome;
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
}
