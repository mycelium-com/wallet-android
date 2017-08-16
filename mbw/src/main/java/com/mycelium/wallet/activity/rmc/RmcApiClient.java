package com.mycelium.wallet.activity.rmc;

import android.util.Log;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.common.net.HttpHeaders;
import com.mrd.bitlib.model.NetworkParameters;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.activity.rmc.json.CreateRmcOrderResponse;
import com.mycelium.wallet.activity.rmc.json.RmcRate;
import com.mycelium.wallet.colu.ColuClient;
import com.mycelium.wapi.wallet.WalletAccount;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RmcApiClient {

    private NetworkParameters network;

    public RmcApiClient(NetworkParameters network) {
        this.network = network;
    }

    private String getApiURL() {
        if (this.network == NetworkParameters.productionNetwork)
            return "https://rmc-ico.gear.mycelium.com/api/";
        //Return TestNet parameters otherwise
        return "https://rmc-ico-test.gear.mycelium.com/api/";
    }

    public CreateRmcOrderResponse.Json createOrder(String amountInRmc, String assetAddress, String paymentMethod) {

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
}
