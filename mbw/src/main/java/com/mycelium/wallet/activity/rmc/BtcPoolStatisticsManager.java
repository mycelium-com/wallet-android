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
import com.mrd.bitlib.model.Address;
import com.mycelium.wallet.colu.ColuAccount;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BtcPoolStatisticsManager {

    public static String HASHRATE_INFO_API_URL = "https://stat.rmc.one/api/stats/hashrate";
    public static String YOUR_RMC_HASHRATE_INFO_URL = "https://stat.rmc.one/api/hashrate/";
    public static String STATS_INFO_API_URL = "https://rmc-ico.gear.mycelium.com/api/stats";

    private ColuAccount coluAccount;

    class PoolStatisticInfo {
        long totalRmcHashrate;
        long yourRmcHashrate;

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
        if (totalRmcHashrate == null)
            totalRmcHashrate = 0L;

        Long yourRmcHashrate = getYourRmcHahrate(coluAccount.getAddress());
        if (yourRmcHashrate == null)
            yourRmcHashrate = 0L;

        return new PoolStatisticInfo(totalRmcHashrate, yourRmcHashrate);
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
        } catch (Exception ex) {
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
        } catch (Exception ex) {
        }
        return null;
    }

    public static String getICOEnd() {
        HttpRequestFactory requestFactory = new NetHttpTransport()
                .createRequestFactory(new HttpRequestInitializer() {
                    @Override
                    public void initialize(HttpRequest request) {
                        request.setParser(new JsonObjectParser(new JacksonFactory()));
                    }
                });
        try {
            HttpRequest request = requestFactory.buildGetRequest(new GenericUrl(STATS_INFO_API_URL));
            HttpResponse response = request.execute();
            InputStream inputStream = response.getContent();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IOUtils.copy(inputStream, baos, true);

            Matcher matcher = Pattern.compile("\"end_at\":\"(.*?)\"").matcher(baos.toString());
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception ex) {
        }
        return null;

    }
}
