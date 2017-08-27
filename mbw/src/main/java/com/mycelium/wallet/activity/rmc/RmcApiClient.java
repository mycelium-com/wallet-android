package com.mycelium.wallet.activity.rmc;

import android.util.Log;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.mrd.bitlib.model.NetworkParameters;
import com.mycelium.wallet.activity.rmc.json.CreateRmcOrderResponse;
import com.mycelium.wallet.activity.rmc.json.RmcRate;

import java.util.HashMap;

public class RmcApiClient {

    private NetworkParameters network;

    public RmcApiClient(NetworkParameters network) {
        this.network = network;
    }

    private String getApiURL() {
        if (this.network == NetworkParameters.productionNetwork)
            return "https://rmc-ico.gear.mycelium.com/api/";
        //Return TestNet parameters otherwise
        if(this.network == NetworkParameters.testNetwork)
            return "https://rmc-ico-test.gear.mycelium.com/api/";
        throw new RuntimeException("can't find network, this never should be happens");
    }

    public boolean isCallbackMine(String callback) {
        return callback != null && callback.contains(getApiURL());
    }

    public CreateRmcOrderResponse.Json createOrder(String amountInRmc, String assetAddress, String paymentMethod, String customerID) {

        HashMap<String, String> data = new HashMap<>();
        data.put("amount_in_rmc", amountInRmc);
        data.put("asset_address", assetAddress);
        data.put("payment_method", paymentMethod);
        data.put("currency", paymentMethod);

        HttpRequestFactory requestFactory = new NetHttpTransport()
                .createRequestFactory(new HttpRequestInitializer() {
                    @Override
                    public void initialize(HttpRequest request) {
                        request.setParser(new JsonObjectParser(new JacksonFactory()));
                    }
                });
        HttpContent content = new JsonHttpContent(new JacksonFactory(), data);

        try {
            HttpRequest request = requestFactory.buildPostRequest(new GenericUrl(getApiURL() + "orders"), content);
            HttpHeaders headers = request.getHeaders();
            headers.set("X-Auth-MWT", customerID);
            HttpResponse response = request.execute();
            return response.parseAs(CreateRmcOrderResponse.Json.class);

        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return null;
    }

    public Float exchangeUsdRmcRate() {

        HttpRequestFactory requestFactory = new NetHttpTransport()
                .createRequestFactory(new HttpRequestInitializer() {
                    @Override
                    public void initialize(HttpRequest request) {
                        request.setParser(new JsonObjectParser(new JacksonFactory()));
                    }
                });

        try {
            HttpRequest request = requestFactory.buildGetRequest(new GenericUrl(getApiURL() + "exchange_rates?pair=USD_RMC"));
            HttpResponse response = request.execute();
            RmcRate[] rate = response.parseAs(RmcRate[].class);
            return rate[0].rate;
        } catch (Exception ex) {
            Log.e("RmcApiClient", "exchangeUsdRmcRate", ex);
        }
        return null;
    }
    public Float exchangeEthUsdRate() {
        HttpRequestFactory requestFactory = new NetHttpTransport()
                .createRequestFactory(new HttpRequestInitializer() {
                    @Override
                    public void initialize(HttpRequest request) {
                        request.setParser(new JsonObjectParser(new JacksonFactory()));
                    }
                });

        try {
            HttpRequest request = requestFactory.buildGetRequest(new GenericUrl(getApiURL() + "exchange_rates?pair=ETH_USD"));
            HttpResponse response = request.execute();
            RmcRate[] rate = response.parseAs(RmcRate[].class);
            return rate[0].rate;
        } catch (Exception ex) {
            Log.e("RmcApiClient", "exchangeUsdRmcRate", ex);
        }
        return null;
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
