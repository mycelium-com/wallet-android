/*
 * Copyright 2013, 2014 Megion Research and Development GmbH
 *
 * Licensed under the Microsoft Reference Source License (MS-RSL)
 *
 * This license governs use of the accompanying software. If you use the software, you accept this license.
 * If you do not accept the license, do not use the software.
 *
 * 1. Definitions
 * The terms "reproduce," "reproduction," and "distribution" have the same meaning here as under U.S. copyright law.
 * "You" means the licensee of the software.
 * "Your company" means the company you worked for when you downloaded the software.
 * "Reference use" means use of the software within your company as a reference, in read only form, for the sole purposes
 * of debugging your products, maintaining your products, or enhancing the interoperability of your products with the
 * software, and specifically excludes the right to distribute the software outside of your company.
 * "Licensed patents" means any Licensor patent claims which read directly on the software as distributed by the Licensor
 * under this license.
 *
 * 2. Grant of Rights
 * (A) Copyright Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free copyright license to reproduce the software for reference use.
 * (B) Patent Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free patent license under licensed patents for reference use.
 *
 * 3. Limitations
 * (A) No Trademark License- This license does not grant you any rights to use the Licensor’s name, logo, or trademarks.
 * (B) If you begin patent litigation against the Licensor over patents that you think may apply to the software
 * (including a cross-claim or counterclaim in a lawsuit), your license to the software ends automatically.
 * (C) The software is licensed "as-is." You bear the risk of using it. The Licensor gives no express warranties,
 * guarantees or conditions. You may have additional consumer rights under your local laws which this license cannot
 * change. To the extent permitted under your local laws, the Licensor excludes the implied warranties of merchantability,
 * fitness for a particular purpose and non-infringement.
 */

package com.mycelium.wallet.bitid;


import android.os.AsyncTask;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.io.CharStreams;
import com.google.common.io.InputSupplier;
import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.crypto.SignedMessage;
import com.mrd.bitlib.model.Address;
import com.mrd.mbwapi.api.MyceliumWalletApi;
import com.mycelium.wallet.AndroidRandomSource;
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
   private InMemoryPrivateKey privateKey;
   private Address address;
   private Bus bus;

   public BitIdAsyncTask(BitIDSignRequest request, boolean enforceSslCorrectness, InMemoryPrivateKey privateKey, Address address, Bus bus) {
      this.enforceSslCorrectness = enforceSslCorrectness;
      this.request = request;
      this.privateKey = privateKey;
      this.address = address;
      this.bus = bus;
   }

   @Override
   protected BitIDResponse doInBackground(Void... params) {
      final BitIDResponse response = new BitIDResponse();
      try {

         SignedMessage signature = privateKey.signMessage(request.getFullUri(), new AndroidRandomSource());

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
         writer.write(request.getCallbackJson(address, signature).toString());
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
