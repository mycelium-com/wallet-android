/*
 * Copyright 2013 Megion Research and Development GmbH
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

package com.mrd.mbwapi.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;

import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.util.ByteReader;
import com.mrd.bitlib.util.ByteWriter;
import com.mrd.bitlib.util.SslUtils;
import com.mrd.mbwapi.api.ApiException;
import com.mrd.mbwapi.api.ApiObject;
import com.mrd.mbwapi.api.BroadcastTransactionRequest;
import com.mrd.mbwapi.api.BroadcastTransactionResponse;
import com.mrd.mbwapi.api.CurrencyCode;
import com.mrd.mbwapi.api.ErrorCollectionRequest;
import com.mrd.mbwapi.api.ErrorCollectionResponse;
import com.mrd.mbwapi.api.ErrorMetaData;
import com.mrd.mbwapi.api.ExchangeSummary;
import com.mrd.mbwapi.api.GetTransactionDataRequest;
import com.mrd.mbwapi.api.GetTransactionDataResponse;
import com.mrd.mbwapi.api.MyceliumWalletApi;
import com.mrd.mbwapi.api.QueryAddressSetStatusRequest;
import com.mrd.mbwapi.api.QueryAddressSetStatusResponse;
import com.mrd.mbwapi.api.QueryBalanceRequest;
import com.mrd.mbwapi.api.QueryBalanceResponse;
import com.mrd.mbwapi.api.QueryExchangeRatesRequest;
import com.mrd.mbwapi.api.QueryExchangeRatesResponse;
import com.mrd.mbwapi.api.QueryExchangeSummaryRequest;
import com.mrd.mbwapi.api.QueryExchangeSummaryResponse;
import com.mrd.mbwapi.api.QueryTransactionInventoryExResponse;
import com.mrd.mbwapi.api.QueryTransactionInventoryRequest;
import com.mrd.mbwapi.api.QueryTransactionInventoryResponse;
import com.mrd.mbwapi.api.QueryTransactionSummaryRequest;
import com.mrd.mbwapi.api.QueryTransactionSummaryResponse;
import com.mrd.mbwapi.api.QueryUnspentOutputsRequest;
import com.mrd.mbwapi.api.QueryUnspentOutputsResponse;
import com.mrd.mbwapi.api.WalletVersionRequest;
import com.mrd.mbwapi.api.WalletVersionResponse;

public class MyceliumWalletApiImpl implements MyceliumWalletApi {

   private static final int VERY_LONG_TIMEOUT_MS = 60000 * 10;
   private static final int LONG_TIMEOUT_MS = 60000;
   private static final int MEDIUM_TIMEOUT_MS = 20000;
   private static final int SHORT_TIMEOUT_MS = 4000;

   @Override
   public ExchangeSummary[] getRate(CurrencyCode currencyCode) throws ApiException {
      return queryExchangeSummary(new QueryExchangeSummaryRequest(currencyCode.getShortString())).exchangeSummaries;
   }

   public static class HttpEndpoint {
      public String baseUrlString;

      public HttpEndpoint(String baseUrlString) {
         this.baseUrlString = baseUrlString;
      }
   }

   public static class HttpsEndpoint extends HttpEndpoint {
      String certificateThumbprint;

      public HttpsEndpoint(String baseUrlString, String certificateThumbprint) {
         super(baseUrlString);
         this.certificateThumbprint = certificateThumbprint;
      }
   }

   private static final String API_PREFIX = "/api/1/request/";
   private HttpEndpoint[] _serverEndpoints;
   private int _currentServerUrlIndex;
   private NetworkParameters _network;

   public MyceliumWalletApiImpl(HttpEndpoint[] serverEndpoints, NetworkParameters network) {
      // Prepare raw URL strings with prefix
      _serverEndpoints = serverEndpoints;
      // Choose a random URL to use
      _currentServerUrlIndex = new Random().nextInt(_serverEndpoints.length);
      _network = network;
   }

   @Override
   public NetworkParameters getNetwork() {
      return _network;
   }

   @Override
   public QueryBalanceResponse queryBalance(QueryBalanceRequest request) throws ApiException {
      HttpURLConnection connection = sendRequest(request, RequestConst.QUERY_BALANCE);
      return receiveResponse(QueryBalanceResponse.class, connection);
   }

   @Override
   public QueryExchangeSummaryResponse queryExchangeSummary(QueryExchangeSummaryRequest request) throws ApiException {
      HttpURLConnection connection = sendRequest(request, RequestConst.LEGACY_QUERY_EXCHANGE_SUMMARY);
      return receiveResponse(QueryExchangeSummaryResponse.class, connection);
   }

   @Override
   public QueryExchangeRatesResponse queryExchangeRates(QueryExchangeRatesRequest request) throws ApiException {
      HttpURLConnection connection = sendRequest(request, RequestConst.QUERY_EXCHANGE_RATES);
      return receiveResponse(QueryExchangeRatesResponse.class, connection);
   }

   @Override
   public QueryUnspentOutputsResponse queryUnspentOutputs(QueryUnspentOutputsRequest request) throws ApiException {
      HttpURLConnection connection = sendRequest(request, RequestConst.QUERY_UNSPENT_OUTPUTS);
      return receiveResponse(QueryUnspentOutputsResponse.class, connection);
   }

   @Override
   public QueryAddressSetStatusResponse queryActiveOutputsInventory(QueryAddressSetStatusRequest request)
         throws ApiException {
      HttpURLConnection connection = sendRequest(request, RequestConst.QUERY_ADDRESS_SET_STATUS);
      return receiveResponse(QueryAddressSetStatusResponse.class, connection);
   }

   @Override
   public GetTransactionDataResponse getTransactionData(GetTransactionDataRequest request) throws ApiException {
      HttpURLConnection connection = sendRequest(request, RequestConst.GET_TRANSACTION_DATA);
      return receiveResponse(GetTransactionDataResponse.class, connection);
   }

   @Override
   public BroadcastTransactionResponse broadcastTransaction(BroadcastTransactionRequest request) throws ApiException {
      HttpURLConnection connection = sendRequest(request, RequestConst.BROADCAST_TRANSACTION);
      return receiveResponse(BroadcastTransactionResponse.class, connection);
   }

   @Override
   public ErrorCollectionResponse collectError(Throwable e, String version, ErrorMetaData metaData) throws ApiException {
      HttpURLConnection connection = sendRequest(new ErrorCollectionRequest(e, version, metaData),
            RequestConst.ERROR_COLLECTOR);
      return receiveResponse(ErrorCollectionResponse.class, connection);
   }

   @Override
   public QueryTransactionInventoryResponse queryTransactionInventory(QueryTransactionInventoryRequest request)
         throws ApiException {
      HttpURLConnection connection = sendRequest(request, RequestConst.QUERY_TRANSACTION_INVENTORY);
      return receiveResponse(QueryTransactionInventoryResponse.class, connection);
   }

   @Override
   public QueryTransactionInventoryExResponse queryTransactionInventoryEx(QueryTransactionInventoryRequest request)
         throws ApiException {
      HttpURLConnection connection = sendRequest(request, RequestConst.QUERY_TRANSACTION_INVENTORY_EX);
      return receiveResponse(QueryTransactionInventoryExResponse.class, connection);
   }

   @Override
   public QueryTransactionSummaryResponse queryTransactionSummary(QueryTransactionSummaryRequest request)
         throws ApiException {
      HttpURLConnection connection = sendRequest(request, RequestConst.QUERY_TRANSACTION_SUMMARY);
      return receiveResponse(QueryTransactionSummaryResponse.class, connection);
   }

   @Override
   public WalletVersionResponse getVersionInfo(WalletVersionRequest request) throws ApiException {
      HttpURLConnection connection = sendRequest(request, RequestConst.WALLET_VERSION);
      return receiveResponse(WalletVersionResponse.class, connection);
   }

   private HttpURLConnection sendRequest(ApiObject request, String function) throws ApiException {
      try {
         HttpURLConnection connection = getConnectionAndSendRequest(request, function);
         if (connection == null) {
            throw new ApiException(MyceliumWalletApi.ERROR_CODE_COMMUNICATION_ERROR, "Unable to connect to the server");
         }
         int status = connection.getResponseCode();
         if (status != 200) {
            throw new ApiException(MyceliumWalletApi.ERROR_CODE_UNEXPECTED_SERVER_RESPONSE, "Unexpected status code: "
                  + status);
         }
         int contentLength = connection.getContentLength();
         if (contentLength == -1) {
            throw new ApiException(MyceliumWalletApi.ERROR_CODE_UNEXPECTED_SERVER_RESPONSE, "Invalid content-length");
         }
         return connection;
      } catch (IOException e) {
         throw new ApiException(MyceliumWalletApi.ERROR_CODE_COMMUNICATION_ERROR, e.getMessage());
      }
   }

   /**
    * Attempt to connect and send to a URL in our list of URLS, if it fails try
    * the next until we have cycled through all URLs. If this fails with a short
    * timeout, retry all servers with a medium timeout, followed by a retry with
    * long timeout.
    */
   private HttpURLConnection getConnectionAndSendRequest(ApiObject request, String function) {
      HttpURLConnection connection;
      connection = getConnectionAndSendRequestWithTimeout(request, function, SHORT_TIMEOUT_MS);
      if (connection != null) {
         return connection;
      }
      connection = getConnectionAndSendRequestWithTimeout(request, function, MEDIUM_TIMEOUT_MS);
      if (connection != null) {
         return connection;
      }
      connection = getConnectionAndSendRequestWithTimeout(request, function, LONG_TIMEOUT_MS);
      if (connection != null) {
         return connection;
      }
      return getConnectionAndSendRequestWithTimeout(request, function, VERY_LONG_TIMEOUT_MS);
   }

   /**
    * Attempt to connect and send to a URL in our list of URLS, if it fails try
    * the next until we have cycled through all URLs. timeout.
    */
   private HttpURLConnection getConnectionAndSendRequestWithTimeout(ApiObject request, String function, int timeout) {
      int originalConnectionIndex = _currentServerUrlIndex;
      while (true) {
         try {
            HttpURLConnection connection = getHttpConnection(_serverEndpoints[_currentServerUrlIndex], function,
                  timeout);
            byte[] toSend = request.serialize(new ByteWriter(1024)).toBytes();
            connection.setRequestProperty("Content-Length", String.valueOf(toSend.length));
            connection.getOutputStream().write(toSend);
            int status = connection.getResponseCode();
            if (status == 200) {
               // If the status code is not 200 we cycle to the next server
               return connection;
            }
            if (status == -1) {
               // We have observed that status -1 might be returned when doing
               // HTTPS session reuse on old android devices.
               // Disabling http.keepAlive fixes it, but it has to be done before any HTTP connections are made.
               // Maybe the caller forgot to call System.setProperty("http.keepAlive", "false"); for old devices?
               System.out.println("HTTP status = -1 Caller may have forgotten to call System.setProperty(\"http.keepAlive\", \"false\"); for old devices");
            }
         } catch (IOException e) {
            e.printStackTrace();
            // handle below like the all status codes != 200
         }
         // Try the next server
         _currentServerUrlIndex = (_currentServerUrlIndex + 1) % _serverEndpoints.length;
         if (_currentServerUrlIndex == originalConnectionIndex) {
            // We have tried all URLs
            return null;
         }

      }
   }

   private <T> T receiveResponse(Class<T> klass, HttpURLConnection connection) throws ApiException {
      try {
         int contentLength = connection.getContentLength();
         byte[] received = readBytes(contentLength, connection.getInputStream());
         T response = ApiObject.deserialize(klass, new ByteReader(received));
         return response;
      } catch (IOException e) {
         throw new ApiException(MyceliumWalletApi.ERROR_CODE_COMMUNICATION_ERROR, e.getMessage());
      }
   }

   private byte[] readBytes(int size, InputStream inputStream) throws IOException {
      byte[] bytes = new byte[size];
      int index = 0;
      int toRead;
      while ((toRead = size - index) > 0) {
         int read = inputStream.read(bytes, index, toRead);
         if (read == -1) {
            throw new IOException();
         }
         index += read;
      }
      return bytes;
   }

   private HttpURLConnection getHttpConnection(HttpEndpoint serverEndpoint, String function, int timeout)
         throws IOException {
      StringBuilder sb = new StringBuilder();
      String spec = sb.append(serverEndpoint.baseUrlString).append(API_PREFIX).append(function).toString();
      URL url = new URL(spec);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      if (serverEndpoint instanceof HttpsEndpoint) {
         SslUtils.configureTrustedCertificate(connection, ((HttpsEndpoint) serverEndpoint).certificateThumbprint);
      }
      connection.setConnectTimeout(timeout);
      connection.setReadTimeout(timeout);
      connection.setDoInput(true);
      connection.setDoOutput(true);
      return connection;
   }

}
