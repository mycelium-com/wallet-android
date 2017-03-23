package com.mycelium.wallet.simplex;


import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.google.common.base.Charsets;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.otto.Bus;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Created by tomb on 11/20/16.
 */

public class SimplexServer {

    private final String baseUrl = "https://mycelium.simplex-affiliates.com";

   private OkHttpClient client = new OkHttpClient();

   public String getAuthRequestUrl() {
      return baseUrl + "/val";
   }

   public void getNonceAsync(final Bus eventBus, final Context context) {
      Request request = new Request.Builder()
              .url(baseUrl + "/android")
              .build();

      client.newCall(request).enqueue(new Callback() {
         @Override
         public void onFailure(Request request, IOException e) {
            Log.w("simplex", "get nonce Failed!");
            SimplexError error = new SimplexError();
            error.activityHandler = new Handler(context.getMainLooper());
            if (!Utils.isConnected(context)) {
               error.message = context.getResources().getString(R.string.no_network_connection);
            } else {
               error.message = e.getMessage();
            }
            eventBus.post(error);
         }

         @Override
         public void onResponse(com.squareup.okhttp.Response response) throws IOException {
            if (!response.isSuccessful()) {
               //change the UI to retry screen
               Log.w("simplex", "get nonce not successful.");
               SimplexError error = new SimplexError();
               eventBus.post(error);
            }

            Log.d("simplex","start parse nonce response...");
            String jsonData = response.body().string();
            try {
                JSONObject Jobject = new JSONObject(jsonData);
                SimplexNonceResponse nonceResponse = new SimplexNonceResponse();
                nonceResponse.simplexNonce = Jobject.getString("nonce");
                nonceResponse.googleNonce =  Jobject.getLong("google");
                eventBus.post(nonceResponse);
            } catch (JSONException e) {
                Log.w("simplex","failed to parse getNonce json!");
                e.printStackTrace();
                SimplexError error = new SimplexError();
                eventBus.post(error);
            }
         }
      });
   }
}
