package com.mycelium.wallet.exchange;

import android.util.Log;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.mycelium.wallet.exchange.model.Rate;

public class BitflipApi {
    private static final String API_RATES = "https://api.bitflip.cc/method/market.getRates";

    public static Rate[] getRates() {
        HttpRequestFactory requestFactory = new NetHttpTransport()
                .createRequestFactory(new HttpRequestInitializer() {
                    @Override
                    public void initialize(HttpRequest request) {
                        request.setParser(new JsonObjectParser(new JacksonFactory()));
                    }
                });

        try {
            HttpRequest request = requestFactory.buildGetRequest(new GenericUrl(API_RATES));
            HttpResponse response = request.execute();
            Rate[][] rate = response.parseAs(Rate[][].class);
            return rate[1];
        } catch (Exception ex) {
            Log.e("BitflipApi", "getRates", ex);
        }
        return null;
    }
}
