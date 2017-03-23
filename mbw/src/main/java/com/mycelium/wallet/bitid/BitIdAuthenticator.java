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


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.crypto.SignedMessage;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.util.SslUtils;
import com.mycelium.wallet.Constants;
import com.mycelium.wallet.bitid.json.BitIdError;
import com.squareup.okhttp.*;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

public class BitIdAuthenticator {

   private boolean enforceSslCorrectness;
   private BitIDSignRequest request;
   private InMemoryPrivateKey privateKey;
   private Address address;

   public BitIdAuthenticator(BitIDSignRequest request, boolean enforceSslCorrectness, InMemoryPrivateKey privateKey, Address address) {
      this.enforceSslCorrectness = enforceSslCorrectness;
      this.request = request;
      this.privateKey = privateKey;
      this.address = address;
   }

   public BitIdResponse queryServer() {
      final BitIdResponse bitIdResponse = new BitIdResponse();
      final SignedMessage signature = privateKey.signMessage(request.getFullUri());
      try {
         OkHttpClient client = getOkHttpClient();
         Request request = getRequest(signature);
         Response callResponse = client.newCall(request).execute();

         bitIdResponse.code = callResponse.code();

         if (bitIdResponse.code >= 200 && bitIdResponse.code < 300) {
            bitIdResponse.status = BitIdResponse.ResponseStatus.SUCCESS;
            bitIdResponse.message = callResponse.body().string();
         } else {
            bitIdResponse.status = BitIdResponse.ResponseStatus.ERROR;
            bitIdResponse.message = formatErrorMessage(callResponse.body().string());
         }
      } catch (InterruptedIOException e) {
         //seems like this can also happen when a timeout occurs
         bitIdResponse.status = BitIdResponse.ResponseStatus.TIMEOUT;
      } catch (UnknownHostException e) {
         //host not known, most probably the device has no internet connection
         bitIdResponse.status = BitIdResponse.ResponseStatus.NOCONNECTION;
      } catch (ConnectException e) {
         //might be a refused connection
         bitIdResponse.status = BitIdResponse.ResponseStatus.REFUSED;
      } catch (SSLException e) {
         Preconditions.checkState(enforceSslCorrectness);
         //ask user whether he wants to proceed although there is a problem with the certificate
         bitIdResponse.message = e.getLocalizedMessage();
         bitIdResponse.status = BitIdResponse.ResponseStatus.SSLPROBLEM;
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
      return bitIdResponse;
   }

   protected String formatErrorMessage(String error) {
      try {
         ObjectMapper objectMapper = new ObjectMapper();
         objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
         BitIdError bitIdError = objectMapper.readValue(error, BitIdError.class);
         if (bitIdError == null){
            // return it verbose
            return error;
         }else{
            return bitIdError.userMessage;
         }

      } catch (IOException e) {
         // return it verbose
         return error;
      }
   }


   protected Request getRequest(SignedMessage signature) {
      MediaType jsonType = MediaType.parse("application/json; charset=utf-8");
      String jsonString = request.getCallbackJson(address, signature).toString();
      RequestBody body = RequestBody.create(jsonType, jsonString);
      String url = request.getCallbackUri();
      return new Request.Builder()
            .url(url)
            .post(body)
            .build();
   }

   private OkHttpClient getOkHttpClient() {
      OkHttpClient client = new OkHttpClient();
      if (!enforceSslCorrectness) {
         //user explicitly agreed to not check certificates
         client.setSslSocketFactory(SslUtils.SSL_SOCKET_FACTORY_ACCEPT_ALL);
         client.setHostnameVerifier(SslUtils.HOST_NAME_VERIFIER_ACCEPT_ALL);
      }
      client.setConnectTimeout(Constants.SHORT_HTTP_TIMEOUT_MS, TimeUnit.MILLISECONDS);
      client.setReadTimeout(Constants.SHORT_HTTP_TIMEOUT_MS, TimeUnit.MILLISECONDS);
      return client;
   }

}
