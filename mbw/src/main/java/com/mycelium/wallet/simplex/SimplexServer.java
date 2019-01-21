package com.mycelium.wallet.simplex;


import android.content.Context;
import android.os.Handler;

import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.otto.Bus;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

class SimplexServer {
    private final String baseUrl = "https://mycelium.simplex-affiliates.com";
    private OkHttpClient client = new OkHttpClient();

    String getAuthRequestUrl() {
        return baseUrl + "/val";
    }

    void getNonceAsync(final Bus eventBus, final Context context) {
        Request request = new Request.Builder()
                .url(baseUrl + "/android")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                String message;
                if (!Utils.isConnected(context)) {
                    message = context.getResources().getString(R.string.no_network_connection);
                } else {
                    message = e.getMessage();
                }
                eventBus.post(new SimplexError(new Handler(context.getMainLooper()), message));
            }

            @Override
            public void onResponse(com.squareup.okhttp.Response response) throws IOException {
                if (!response.isSuccessful()) {
                    //change the UI to retry screen
                    String message = "get nonce not successful.";
                    SimplexError error = new SimplexError(new Handler(context.getMainLooper()), message);
                    eventBus.post(error);
                }

                String jsonData = response.body().string();
                try {
                    JSONObject Jobject = new JSONObject(jsonData);
                    SimplexNonceResponse nonceResponse = new SimplexNonceResponse();
                    nonceResponse.simplexNonce = Jobject.getString("nonce");
                    nonceResponse.googleNonce = Jobject.getLong("google");
                    eventBus.post(nonceResponse);
                } catch (JSONException e) {
                    String message = "failed to parse getNonce json!";
                    SimplexError error = new SimplexError(new Handler(context.getMainLooper()), message);
                    eventBus.post(error);
                }
            }
        });
    }
}
