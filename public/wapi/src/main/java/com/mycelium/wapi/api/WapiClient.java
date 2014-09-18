/*
 * Copyright 2013, 2014 Megion Research & Development GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mycelium.wapi.api;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mrd.bitlib.util.SslUtils;
import com.mycelium.wapi.api.WapiConst.Function;
import com.mycelium.wapi.api.request.*;
import com.mycelium.wapi.api.response.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;

public class WapiClient implements Wapi {

   private static final int VERY_LONG_TIMEOUT_MS = 60000 * 10;
   private static final int LONG_TIMEOUT_MS = 60000;
   private static final int MEDIUM_TIMEOUT_MS = 20000;
   private static final int SHORT_TIMEOUT_MS = 4000;

   public static class HttpEndpoint {
      public final String baseUrlString;

      public HttpEndpoint(String baseUrlString) {
         this.baseUrlString = baseUrlString;
      }

      @Override
      public String toString() {
         return baseUrlString;
      }
   }

   public static class HttpsEndpoint extends HttpEndpoint {
      public final String certificateThumbprint;

      public HttpsEndpoint(String baseUrlString, String certificateThumbprint) {
         super(baseUrlString);
         this.certificateThumbprint = certificateThumbprint;
      }
   }

   private ObjectMapper _objectMapper;
   private WapiLogger _logger;

   private HttpEndpoint[] _serverEndpoints;
   private int _currentServerUrlIndex;

   public WapiClient(HttpEndpoint[] serverEndpoints, WapiLogger logger) {
      _serverEndpoints = serverEndpoints;
      // Choose a random endpoint to use
      _currentServerUrlIndex = new Random().nextInt(_serverEndpoints.length);
      _objectMapper = new ObjectMapper();
      // We ignore properties that do not map onto the version of the class we
      // deserialize
      _objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      _objectMapper.registerModule(new WapiJsonModule());
      _logger = logger;
   }

   private <T> WapiResponse<T> sendRequest(String function, Object request, TypeReference<WapiResponse<T>> typeReference) {
      try {
         HttpURLConnection connection = getConnectionAndSendRequest(function, request);
         if (connection == null) {
            return new WapiResponse<T>(ERROR_CODE_NO_SERVER_CONNECTION, null);
         }
         return _objectMapper.readValue(connection.getInputStream(), typeReference);
      } catch (JsonParseException e) {
         logError("sendRequest failed with Json parsing error.", e);
         return new WapiResponse<T>(ERROR_CODE_INTERNAL_CLIENT_ERROR, null);
      } catch (JsonMappingException e) {
         logError("sendRequest failed with Json mapping error.", e);
         return new WapiResponse<T>(ERROR_CODE_INTERNAL_CLIENT_ERROR, null);
      } catch (IOException e) {
         logError("sendRequest failed IO exception.", e);
         return new WapiResponse<T>(ERROR_CODE_INTERNAL_CLIENT_ERROR, null);
      }
   }

   private void logError(String message) {
      if (_logger != null) {
         _logger.logError(message);
      }
   }

   private void logError(String message, Exception e) {
      if (_logger != null) {
         _logger.logError(message, e);
      }
   }

   /**
    * Attempt to connect and send to a URL in our list of URLS, if it fails try
    * the next until we have cycled through all URLs. If this fails with a short
    * timeout, retry all servers with a medium timeout, followed by a retry with
    * long timeout.
    */
   private HttpURLConnection getConnectionAndSendRequest(String function, Object request) {
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
   private HttpURLConnection getConnectionAndSendRequestWithTimeout(Object request, String function, int timeout) {
      int originalConnectionIndex = _currentServerUrlIndex;
      while (true) {
         try {
            HttpURLConnection connection = getHttpConnection(_serverEndpoints[_currentServerUrlIndex], function,
                  timeout);

            byte[] toSend = getPostBytes(request);
            connection.setRequestProperty("Content-Length", String.valueOf(toSend.length));
            connection.setRequestProperty("Content-Type", "application/json");
            connection.getOutputStream().write(toSend);
            int status = connection.getResponseCode();

            // Check for status code 2XX
            if (status / 100 == 2) {
               // If the status code is not 200 we cycle to the next server
               return connection;
            }
            if (status == -1) {
               // We have observed that status -1 might be returned when doing
               // HTTPS session reuse on old android devices.
               // Disabling http.keepAlive fixes it, but it has to be done
               // before any HTTP connections are made.
               // Maybe the caller forgot to call
               // System.setProperty("http.keepAlive", "false"); for old
               // devices?
               logError("HTTP status = -1 Caller may have forgotten to call System.setProperty(\"http.keepAlive\", \"false\"); for old devices");
            }
         } catch (IOException e) {
            logError("IOException when sending request", e);
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

   private byte[] getPostBytes(Object request) {

      try {
         String postString = _objectMapper.writeValueAsString(request);
         return postString.getBytes("UTF-8");
      } catch (JsonProcessingException e) {
         logError("Error during JSON serialization", e);
         throw new RuntimeException(e);
      } catch (UnsupportedEncodingException e) {
         // Never happens
         logError("Error during binary serialization", e);
         throw new RuntimeException(e);
      }
   }

   private HttpURLConnection getHttpConnection(HttpEndpoint endpoint, String function, int timeout) throws IOException {
      URL url = new URL(endpoint.baseUrlString + WapiConst.WAPI_BASE_PATH + '/' + function);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      if (endpoint instanceof HttpsEndpoint) {
         SslUtils.configureTrustedCertificate(connection, ((HttpsEndpoint) (endpoint)).certificateThumbprint);
      }
      connection.setConnectTimeout(timeout);
      connection.setReadTimeout(timeout);
      connection.setDoInput(true);
      connection.setDoOutput(true);
      return connection;
   }

   @Override
   public WapiResponse<QueryUnspentOutputsResponse> queryUnspentOutputs(QueryUnspentOutputsRequest request) {
      return sendRequest(Function.QUERY_UNSPENT_OUTPUTS, request,
            new TypeReference<WapiResponse<QueryUnspentOutputsResponse>>() {
            });
   }

   @Override
   public WapiResponse<QueryTransactionInventoryResponse> queryTransactionInventory(
         QueryTransactionInventoryRequest request) {
      return sendRequest(Function.QUERY_TRANSACTION_INVENTORY, request,
            new TypeReference<WapiResponse<QueryTransactionInventoryResponse>>() {
            });
   }

   @Override
   public WapiResponse<GetTransactionsResponse> getTransactions(GetTransactionsRequest request) {
      TypeReference<WapiResponse<GetTransactionsResponse>> typeref = new TypeReference<WapiResponse<GetTransactionsResponse>>() {
      };
      return sendRequest(Function.GET_TRANSACTIONS, request, typeref);
   }

   @Override
   public WapiResponse<BroadcastTransactionResponse> broadcastTransaction(BroadcastTransactionRequest request) {
      return sendRequest(Function.BROADCAST_TRANSACTION, request,
            new TypeReference<WapiResponse<BroadcastTransactionResponse>>() {
            });
   }

   @Override
   public WapiResponse<CheckTransactionsResponse> checkTransactions(CheckTransactionsRequest request) {
      TypeReference<WapiResponse<CheckTransactionsResponse>> typeref = new TypeReference<WapiResponse<CheckTransactionsResponse>>() {
      };
      return sendRequest(Function.CHECK_TRANSACTIONS, request, typeref);
   }

   @Override
   public WapiResponse<QueryExchangeRatesResponse> queryExchangeRates(QueryExchangeRatesRequest request) {
      TypeReference<WapiResponse<QueryExchangeRatesResponse>> typeref = new TypeReference<WapiResponse<QueryExchangeRatesResponse>>() {
      };
      return sendRequest(Function.QUERY_EXCHANGE_RATES, request, typeref);
   }

   @Override
   public  WapiResponse<PingResponse> ping(){
      TypeReference<WapiResponse<PingResponse>> typeref = new TypeReference<WapiResponse<PingResponse>>() { };
      return sendRequest(Function.PING, null, typeref);
   }
 

   @Override
   public WapiLogger getLogger() {
      return _logger;
   }

}
