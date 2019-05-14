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
import com.mycelium.WapiLogger;
import com.mycelium.net.*;
import com.mycelium.wapi.api.WapiConst.Function;
import com.mycelium.wapi.api.request.*;
import com.mycelium.wapi.api.response.*;
import com.squareup.okhttp.*;


import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;


public abstract class WapiClient implements Wapi, WapiClientLifecycle {
   private static final int VERY_LONG_TIMEOUT_MS = 10 * 60 * 1000; // 10 minutes
   private static final int LONG_TIMEOUT_MS = 60 * 1000; // one minute
   private static final int MEDIUM_TIMEOUT_MS = 20 * 1000; // 20s
   private static final int SHORT_TIMEOUT_MS = 4 * 1000; // 4s
   private static final int[] SHORT_TO_LONG_TIMEOUTS_MS = new int[]{
      SHORT_TIMEOUT_MS, MEDIUM_TIMEOUT_MS, LONG_TIMEOUT_MS, VERY_LONG_TIMEOUT_MS
   };

   // the minimum timeout for the lifetime of this process(?). Restarting the app resets it to the minimum.
   private static int _minTimeout = 0;

   private ObjectMapper _objectMapper;
   private com.mycelium.WapiLogger _logger;

   private ServerEndpoints _serverEndpoints;
   private String versionCode;

   public WapiClient(ServerEndpoints serverEndpoints, WapiLogger logger, String versionCode) {
      _serverEndpoints = serverEndpoints;
      this.versionCode = versionCode;

      // Choose a random endpoint to use
      _objectMapper = new ObjectMapper();
      // We ignore properties that do not map onto the version of the class we
      // deserialize
      _objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      _objectMapper.registerModule(new WapiJsonModule());
      _logger = logger;
   }

   private <T> WapiResponse<T> sendRequest(String function, Object request, TypeReference<WapiResponse<T>> typeReference) {
      try {
         Response response = getConnectionAndSendRequest(function, request);
         if (response == null) {
            return new WapiResponse<T>(ERROR_CODE_NO_SERVER_CONNECTION, null);
         }
         return _objectMapper.readValue(response.body().charStream(), typeReference);
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
   private Response getConnectionAndSendRequest(String function, Object request) {
      for(int timeout: SHORT_TO_LONG_TIMEOUTS_MS) {
         if(timeout < _minTimeout) {
            // if some timeout was too short for all servers, maybe we hit all of them but were slow ourselves
            continue;
         }
         Response response = getConnectionAndSendRequestWithTimeout(request, function, timeout);
         if (response != null) {
            if(timeout > _minTimeout) {
               _minTimeout = timeout;
            }
            return response;
         }
      }
      return null;
   }

   /**
    * Attempt to connect and send to a URL in our list of URLS, if it fails try
    * the next until we have cycled through all URLs. timeout.
    */
   private Response getConnectionAndSendRequestWithTimeout(Object request, String function, int timeout) {
      int originalConnectionIndex = _serverEndpoints.getCurrentEndpointIndex();
      while (true) {
         // currently active server-endpoint
         HttpEndpoint serverEndpoint = _serverEndpoints.getCurrentEndpoint();
         try {
            OkHttpClient client = serverEndpoint.getClient();
            _logger.logInfo("Connecting to " + serverEndpoint.getBaseUrl() + " (" + _serverEndpoints.getCurrentEndpointIndex() + ")");

            client.setConnectTimeout(timeout, TimeUnit.MILLISECONDS);
            client.setReadTimeout(timeout, TimeUnit.MILLISECONDS);
            client.setWriteTimeout(timeout, TimeUnit.MILLISECONDS);

            long callStart = System.currentTimeMillis();
            // build request
            final String toSend = getPostBody(request);
            Request rq = new Request.Builder()
                  .addHeader(MYCELIUM_VERSION_HEADER, versionCode)
                  .post(RequestBody.create(MediaType.parse("application/json"), toSend))
                  .url(serverEndpoint.getUri(WapiConst.WAPI_BASE_PATH, function).toString())
                  .build();

            // execute request
            Response response = client.newCall(rq).execute();
            _logger.logInfo(String.format(Locale.ENGLISH, "Wapi %s finished (%dms)", function, System.currentTimeMillis() - callStart));

            // Check for status code 2XX
            if (response.isSuccessful()) {
               if (serverEndpoint instanceof FeedbackEndpoint){
                  ((FeedbackEndpoint) serverEndpoint).onSuccess();
               }
               return response;
            } else {
               // If the status code is not 200 we cycle to the next server
               logError(String.format(Locale.ENGLISH, "Http call to %s failed with %d %s", function, response.code(), response.message()));
               // throw...
            }
         } catch (IOException e) {
            logError("IOException when sending request " + function, e);
            if (serverEndpoint instanceof FeedbackEndpoint){
               _logger.logInfo("Resetting tor");
               ((FeedbackEndpoint) serverEndpoint).onError();
            }
         } catch (RuntimeException e) {
            logError("Send request fail", e);
         }
         // Try the next server
         _serverEndpoints.switchToNextEndpoint();
         if (_serverEndpoints.getCurrentEndpointIndex() == originalConnectionIndex) {
            // We have tried all URLs
            return null;
         }
      }
   }

   private String getPostBody(Object request) {
      if (request == null) {
         return "";
      }
      try {
         return _objectMapper.writeValueAsString(request);
      } catch (JsonProcessingException e) {
         logError("Error during JSON serialization", e);
         throw new RuntimeException(e);
      }
   }

   @Override
   public WapiResponse<QueryExchangeRatesResponse> queryExchangeRates(QueryExchangeRatesRequest request) {
      TypeReference<WapiResponse<QueryExchangeRatesResponse>> typeref = new TypeReference<WapiResponse<QueryExchangeRatesResponse>>() { };
      return sendRequest(Function.QUERY_EXCHANGE_RATES, request, typeref);
   }

   @Override
   public  WapiResponse<PingResponse> ping(){
      TypeReference<WapiResponse<PingResponse>> typeref = new TypeReference<WapiResponse<PingResponse>>() { };
      return sendRequest(Function.PING, null, typeref);
   }

   @Override
   public WapiResponse<ErrorCollectorResponse> collectError(ErrorCollectorRequest request) {
      TypeReference<WapiResponse<ErrorCollectorResponse>> typeref = new TypeReference<WapiResponse<ErrorCollectorResponse>>() { };
      return sendRequest(Function.COLLECT_ERROR, request, typeref);
   }

   @Override
   public WapiResponse<VersionInfoExResponse> getVersionInfoEx(VersionInfoExRequest request) {
      TypeReference<WapiResponse<VersionInfoExResponse>> typeref = new TypeReference<WapiResponse<VersionInfoExResponse>>() { };
      return sendRequest(Function.GET_VERSION_INFO_EX, request, typeref);
   }

   @Override
   public WapiLogger getLogger() {
      return _logger;
   }
}
