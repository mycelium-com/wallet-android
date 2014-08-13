package com.mycelium.wallet.bitid;


import android.os.AsyncTask;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.io.CharStreams;
import com.google.common.io.InputSupplier;
import com.mrd.bitlib.crypto.SignedMessage;
import com.mrd.mbwapi.api.MyceliumWalletApi;
import com.mycelium.wallet.AndroidRandomSource;
import com.mycelium.wallet.Record;
import com.squareup.otto.Bus;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;

import javax.net.ssl.SSLException;

//AsyncTask to calculate signature and contact server
public class BitIdAsyncTask extends AsyncTask<Void, Integer, BitIDResponse> {

   private boolean enforceSslCorrectness;
   private BitIDSignRequest request;
   private Record record;
   private Bus bus;

   public BitIdAsyncTask(BitIDSignRequest request, boolean enforceSslCorrectness, Record record, Bus bus) {
      this.enforceSslCorrectness = enforceSslCorrectness;
      this.request = request;
      this.record = record;
      this.bus = bus;
   }

   @Override
   protected BitIDResponse doInBackground(Void... params) {
      final BitIDResponse response = new BitIDResponse();
      try {

         SignedMessage signature = record.key.signMessage(request.getFullUri(), new AndroidRandomSource());

         final HttpURLConnection conn = (HttpURLConnection) new URL(request.getCallbackUri()).openConnection();
         //todo evaluate enforceSslCorrectness to disable verification stuff if false (and remove setting it to true)
         enforceSslCorrectness = true;
         conn.setReadTimeout(MyceliumWalletApi.SHORT_TIMEOUT_MS);
         conn.setConnectTimeout(MyceliumWalletApi.SHORT_TIMEOUT_MS);
         conn.setRequestMethod("POST");
         conn.setRequestProperty("Content-Type", "application/json");
         conn.setDoInput(true);
         conn.setDoOutput(true);
         OutputStream os = conn.getOutputStream();
         BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
         writer.write(request.getCallbackJson(record.address, signature).toString());
         writer.flush();
         writer.close();
         os.close();

         conn.connect();
         try {
            response.code = conn.getResponseCode();
         } catch (IOException ioe) {
            //yes, this seems weird, but there might be a IOException in case of a 401 response, and afterwards the object is in a stable state again
            response.code = conn.getResponseCode();
         }
         response.message = CharStreams.toString(CharStreams.newReaderSupplier(new InputSupplier<InputStream>() {
            @Override
            public InputStream getInput() throws IOException {
               if (response.code >= 200 && response.code < 300) {
                  return conn.getInputStream();
               }
               return conn.getErrorStream();
            }
         }, Charsets.UTF_8));

         if (response.code >= 200 && response.code < 300) {
            response.status = BitIDResponse.ResponseStatus.SUCCESS;
         } else {
            response.status = BitIDResponse.ResponseStatus.ERROR;
         }

      } catch (SocketTimeoutException e) {
         //connection timed out
         response.status = BitIDResponse.ResponseStatus.TIMEOUT;
      } catch (UnknownHostException e) {
         //host not known, most probably the device has no internet connection
         response.status = BitIDResponse.ResponseStatus.NOCONNECTION;
      } catch (SSLException e) {
         Preconditions.checkState(enforceSslCorrectness);
         //ask user whether he wants to proceed although there is a problem with the certificate
         response.message = e.getLocalizedMessage();
         response.status = BitIDResponse.ResponseStatus.SSLPROBLEM;
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
      return response;
   }

   @Override
   protected void onPostExecute(BitIDResponse bitIDResponse) {
      bus.post(bitIDResponse);
   }
}
