package com.mycelium.wallet.activity.rmc;

import android.util.Log;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.mrd.bitlib.model.NetworkParameters;
import com.mycelium.wallet.BuildConfig;
import com.mycelium.wallet.activity.rmc.json.RmcRate;

public class RmcApiClient {

    public RmcApiClient(NetworkParameters network) {
    }

    private String getApiURL() {
        return BuildConfig.RMCApiClientURL;
    }

    public Float exchangeBtcUsdRate() {
        HttpRequestFactory requestFactory = new NetHttpTransport()
                .createRequestFactory(new HttpRequestInitializer() {
                    @Override
                    public void initialize(HttpRequest request) {
                        request.setParser(new JsonObjectParser(new JacksonFactory()));
                    }
                });

        try {
            HttpRequest request = requestFactory.buildGetRequest(new GenericUrl(getApiURL() + "exchange_rates?pair=BTC_USD"));
            HttpResponse response = request.execute();
            RmcRate[] rate = response.parseAs(RmcRate[].class);
            return rate[0].rate;
        } catch (Exception ex) {
            Log.e("RmcApiClient", "exchangeUsdRmcRate", ex);
        }
        return null;
    }
}
