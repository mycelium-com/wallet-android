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

package com.mycelium.lt.api;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.util.BitlibJsonModule;
import com.mycelium.lt.api.LtConst.Function;
import com.mycelium.lt.api.LtConst.Param;
import com.mycelium.lt.api.model.*;
import com.mycelium.lt.api.params.AdParameters;
import com.mycelium.lt.api.params.BtcSellPriceParameters;
import com.mycelium.lt.api.params.CreateTradeParameters;
import com.mycelium.lt.api.params.EncryptedChatMessageParameters;
import com.mycelium.lt.api.params.InstantBuyOrderParameters;
import com.mycelium.lt.api.params.LoginParameters;
import com.mycelium.lt.api.params.ReleaseBtcParameters;
import com.mycelium.lt.api.params.SearchParameters;
import com.mycelium.lt.api.params.SetTradeReceivingAddressParameters;
import com.mycelium.lt.api.params.TradeChangeParameters;
import com.mycelium.lt.api.params.TraderParameters;
import com.mycelium.net.HttpEndpoint;
import com.mycelium.net.FeedbackEndpoint;
import com.mycelium.net.ServerEndpoints;
import com.squareup.okhttp.*;

public class LtApiClient implements LtApi {
   private static final long TIMEOUT_MS = TimeUnit.MINUTES.toMillis(2);

   public interface Logger {
      void logError(String message, Exception e);
      void logError(String message);
      void logInfo(String message);
   }

   protected static byte[] uuidToBytes(UUID uuid) {
      ByteArrayOutputStream ba = new ByteArrayOutputStream(16);
      DataOutputStream da = new DataOutputStream(ba);
      try {
         da.writeLong(uuid.getMostSignificantBits());
         da.writeLong(uuid.getLeastSignificantBits());
      } catch (IOException e) {
         // Never happens
      }
      return ba.toByteArray();
   }

   private ServerEndpoints _serverEndpoints;
   private ObjectMapper _objectMapper;
   private Logger _logger;

   public LtApiClient(ServerEndpoints serverEndpoints, Logger logger) {
      _serverEndpoints = serverEndpoints;

      _objectMapper = new ObjectMapper();
      // We ignore properties that do not map onto the version of the class we
      // deserialize
      _objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      _objectMapper.registerModule(new BitlibJsonModule());
      _logger = logger;
   }

   private HttpEndpoint getEndpoint() {
      return _serverEndpoints.getCurrentEndpoint();
   }

   private <T> LtResponse<T> sendRequest(LtRequest request, TypeReference<LtResponse<T>> typeReference) {
      try {
         Response response = getConnectionAndSendRequest(request, TIMEOUT_MS);
         if (response == null) {
            return new LtResponse<>(ERROR_CODE_NO_SERVER_CONNECTION, null);
         }
         String retVal = response.body().string();
         return _objectMapper.readValue(retVal, typeReference);
      } catch (JsonParseException e) {
         logError("sendRequest failed with Json parsing error.", e);
         return new LtResponse<>(ERROR_CODE_INTERNAL_CLIENT_ERROR, null);
      } catch (JsonMappingException e) {
         logError("sendRequest failed with Json mapping error.", e);
         return new LtResponse<>(ERROR_CODE_INTERNAL_CLIENT_ERROR, null);
      } catch (IOException e) {
         logError("sendRequest failed IO exception.", e);
         return new LtResponse<>(ERROR_CODE_INTERNAL_CLIENT_ERROR, null);
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

   private Response getConnectionAndSendRequest(LtRequest request, long timeout) {
      int originalConnectionIndex = _serverEndpoints.getCurrentEndpointIndex();

      while (true) {
         HttpEndpoint serverEndpoint = _serverEndpoints.getCurrentEndpoint();
         try {
            OkHttpClient client = serverEndpoint.getClient();
            _logger.logInfo("LT connecting to " + serverEndpoint.getBaseUrl() + " (" + _serverEndpoints.getCurrentEndpointIndex() + ")");

            // configure TimeOuts
            client.setConnectTimeout(timeout, TimeUnit.MILLISECONDS);
            client.setReadTimeout(timeout, TimeUnit.MILLISECONDS);
            client.setWriteTimeout(timeout, TimeUnit.MILLISECONDS);

            Stopwatch callDuration = Stopwatch.createStarted();
            // build request
            final String toSend = getPostBody(request);
            Request rq = new Request.Builder()
                  .post(RequestBody.create(MediaType.parse("application/json"), toSend))
                  .url(serverEndpoint.getUri(request.toString()).toString())
                  .build();

            // execute request
            Response response = client.newCall(rq).execute();
            callDuration.stop();
            _logger.logInfo(String.format(Locale.US, "LtApi %s finished (%dms)", request.toString(), callDuration.elapsed(TimeUnit.MILLISECONDS)));


            // Check for status code 2XX
            if (response.isSuccessful()) {
               if (serverEndpoint instanceof FeedbackEndpoint){
                  ((FeedbackEndpoint) serverEndpoint).onSuccess();
               }
               return response;
            }else{
               // If the status code is not 200 we cycle to the next server
               logError(String.format(Locale.US, "Local Trader server request for class %s returned HTTP status code %d", request.getClass().toString(), response.code()));
            }

         } catch (IOException e) {
            logError("getConnectionAndSendRequest failed IO exception.");
            if (serverEndpoint instanceof FeedbackEndpoint){
               _logger.logInfo("Resetting tor");
               ((FeedbackEndpoint) serverEndpoint).onError();
            }
         }

         // We had an IO exception or a bad status, fail over and try again
         _serverEndpoints.switchToNextEndpoint();
         // Check if we are back at the initial endpoint, in which case we have
         // to give up
         if (_serverEndpoints.getCurrentEndpointIndex() == originalConnectionIndex) {
            // We have tried all URLs
            return null;
         }
      }
   }

   private String getPostBody(LtRequest request) {
      return request.getPostString();
   }

   public String getUrl(){
      return getEndpoint().getBaseUrl();
   }

   @Override
   public LtResponse<LtSession> createSession(int apiVersion, String locale, String bitcoinDenomination) {
      LtRequest r = new LtRequest(Function.CREATE_SESSION);
      r.addQueryParameter(Param.API_VERSION, Integer.toString(apiVersion));
      r.addQueryParameter(Param.LOCALE, locale);
      r.addQueryParameter(Param.BITCOIN_DENOMINATION, bitcoinDenomination);
      return sendRequest(r, new TypeReference<LtResponse<LtSession>>() {
      });
   }

   @Override
   public LtResponse<Void> createTrader(UUID sessionId, TraderParameters params) {
      LtRequest r = new LtRequest(Function.CREATE_TRADER);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      r.setPostObject(_objectMapper, params);
      return sendRequest(r, new TypeReference<LtResponse<Void>>() {
      });
   }

   @Override
   public LtResponse<String> traderLogin(UUID sessionId, LoginParameters params) {
      LtRequest r = new LtRequest(Function.TRADER_LOGIN);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      r.setPostObject(_objectMapper, params);
      return sendRequest(r, new TypeReference<LtResponse<String>>() {
      });
   }

   @Override
   public LtResponse<Collection<Ad>> listAds(UUID sessionId) {
      LtRequest r = new LtRequest(Function.LIST_ADS);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      return sendRequest(r, new TypeReference<LtResponse<Collection<Ad>>>() {
      });
   }

   @Override
   public LtResponse<List<PriceFormula>> getSupportedPriceFormulas(UUID sessionId) {
      LtRequest r = new LtRequest(Function.GET_SUPPORTED_PRICE_FORMULAS);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      return sendRequest(r, new TypeReference<LtResponse<List<PriceFormula>>>() {
      });
   }

   @Override
   public LtResponse<UUID> createAd(UUID sessionId, AdParameters params) {
      LtRequest r = new LtRequest(Function.CREATE_AD);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      r.setPostObject(_objectMapper, params);
      return sendRequest(r, new TypeReference<LtResponse<UUID>>() {
      });
   }

   @Override
   public LtResponse<SellOrder> getSellOrder(UUID sessionId, UUID sellOrderId) {
      LtRequest r = new LtRequest(Function.GET_SELL_ORDER);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      r.addQueryParameter(Param.SELL_ORDER_ID, sellOrderId.toString());
      return sendRequest(r, new TypeReference<LtResponse<SellOrder>>() {
      });
   }

   @Override
   public LtResponse<Ad> getAd(UUID sessionId, UUID adId) {
      LtRequest r = new LtRequest(Function.GET_AD);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      r.addQueryParameter(Param.AD_ID, adId.toString());
      return sendRequest(r, new TypeReference<LtResponse<Ad>>() {
      });
   }

   @Override
   public LtResponse<Void> editAd(UUID sessionId, UUID adId, AdParameters params) {
      LtRequest r = new LtRequest(Function.EDIT_AD);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      r.addQueryParameter(Param.AD_ID, adId.toString());
      r.setPostObject(_objectMapper, params);
      return sendRequest(r, new TypeReference<LtResponse<Void>>() {
      });
   }

   @Override
   public LtResponse<Void> activateAd(UUID sessionId, UUID adId) {
      LtRequest r = new LtRequest(Function.ACTIVATE_AD);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      r.addQueryParameter(Param.AD_ID, adId.toString());
      return sendRequest(r, new TypeReference<LtResponse<Void>>() {
      });
   }

   @Override
   public LtResponse<Void> deactivateSellOrder(UUID sessionId, UUID sellOrderId) {
      LtRequest r = new LtRequest(Function.DEACTIVATE_SELL_ORDER);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      r.addQueryParameter(Param.SELL_ORDER_ID, sellOrderId.toString());
      return sendRequest(r, new TypeReference<LtResponse<Void>>() {
      });
   }

   @Override
   public LtResponse<Void> deactivateAd(UUID sessionId, UUID adId) {
      LtRequest r = new LtRequest(Function.DEACTIVATE_AD);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      r.addQueryParameter(Param.AD_ID, adId.toString());
      return sendRequest(r, new TypeReference<LtResponse<Void>>() {
      });
   }

   @Override
   public LtResponse<Void> deleteSellOrder(UUID sessionId, UUID sellOrderId) {
      LtRequest r = new LtRequest(Function.DELETE_SELL_ORDER);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      r.addQueryParameter(Param.SELL_ORDER_ID, sellOrderId.toString());
      return sendRequest(r, new TypeReference<LtResponse<Void>>() {
      });
   }

   @Override
   public LtResponse<Void> deleteAd(UUID sessionId, UUID adId) {
      LtRequest r = new LtRequest(Function.DELETE_AD);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      r.addQueryParameter(Param.AD_ID, adId.toString());
      return sendRequest(r, new TypeReference<LtResponse<Void>>() {
      });
   }

   @Override
   public LtResponse<List<SellOrderSearchItem>> sellOrderSearch(UUID sessionId, SearchParameters params) {
      LtRequest r = new LtRequest(Function.SELL_ORDER_SEARCH);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      r.setPostObject(_objectMapper, params);
      return sendRequest(r, new TypeReference<LtResponse<List<SellOrderSearchItem>>>() {
      });
   }

   @Override
   public LtResponse<List<AdSearchItem>> adSearch(UUID sessionId, SearchParameters params) {
      LtRequest r = new LtRequest(Function.AD_SEARCH);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      r.setPostObject(_objectMapper, params);
      return sendRequest(r, new TypeReference<LtResponse<List<AdSearchItem>>>() {
      });
   }

   @Override
   public LtResponse<List<AdSearchItem>> getActiveAds() {
      LtRequest r = new LtRequest(Function.GET_ACTIVE_ADS);
      return sendRequest(r, new TypeReference<LtResponse<List<AdSearchItem>>>() {
      });
   }

   @Override
   public LtResponse<UUID> createInstantBuyOrder(UUID sessionId, InstantBuyOrderParameters params) {
      LtRequest r = new LtRequest(Function.CREATE_INSTANT_BUY_ORDER);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      r.setPostObject(_objectMapper, params);
      return sendRequest(r, new TypeReference<LtResponse<UUID>>() {
      });
   }

   @Override
   public LtResponse<UUID> createTrade(UUID sessionId, CreateTradeParameters params) {
      LtRequest r = new LtRequest(Function.CREATE_TRADE);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      r.setPostObject(_objectMapper, params);
      return sendRequest(r, new TypeReference<LtResponse<UUID>>() {
      });
   }

   @Override
   public LtResponse<LinkedList<TradeSession>> getActiveTradeSessions(UUID sessionId) {
      LtRequest r = new LtRequest(Function.GET_ACTIVE_TRADE_SESSIONS);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      return sendRequest(r, new TypeReference<LtResponse<LinkedList<TradeSession>>>() {
      });
   }

   @Override
   public LtResponse<LinkedList<TradeSession>> getFinalTradeSessions(UUID sessionId, int limit, int offset) {
      LtRequest r = new LtRequest(Function.GET_FINAL_TRADE_SESSIONS);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      r.addQueryParameter(Param.LIMIT, Integer.toString(limit));
      r.addQueryParameter(Param.OFFSET, Integer.toString(offset));
      return sendRequest(r, new TypeReference<LtResponse<LinkedList<TradeSession>>>() {
      });
   }

   @Override
   public LtResponse<LinkedList<TradeSession>> getTradeSessions(UUID sessionId, int limit, int offset) {
      LtRequest r = new LtRequest(Function.GET_TRADE_SESSIONS);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      r.addQueryParameter(Param.LIMIT, Integer.toString(limit));
      r.addQueryParameter(Param.OFFSET, Integer.toString(offset));
      return sendRequest(r, new TypeReference<LtResponse<LinkedList<TradeSession>>>() {
      });
   }

   @Override
   public LtResponse<TradeSession> getTradeSession(UUID sessionId, UUID tradeSessionId) {
      LtRequest r = new LtRequest(Function.GET_TRADE_SESSION);
      r.addQueryParameter("sessionId", sessionId.toString()).addQueryParameter(Param.TRADE_SESSION_ID,
            tradeSessionId.toString());
      return sendRequest(r, new TypeReference<LtResponse<TradeSession>>() {
      });
   }

   @Override
   public LtResponse<Void> setTradeReceivingAddress(UUID sessionId, SetTradeReceivingAddressParameters params) {
      LtRequest r = new LtRequest(Function.SET_TRADE_RECEIVING_ADDRESS);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      r.setPostObject(_objectMapper, params);
      return sendRequest(r, new TypeReference<LtResponse<Void>>() {
      });
   }

   @Override
   public LtResponse<Void> acceptTrade(UUID sessionId, UUID tradeSessionId, long tradeSessionTimestamp) {
      LtRequest r = new LtRequest(Function.ACCEPT_TRADE);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      r.addQueryParameter(Param.TRADE_SESSION_ID, tradeSessionId.toString());
      r.addQueryParameter(Param.TIMESTAMP, Long.toString(tradeSessionTimestamp));
      return sendRequest(r, new TypeReference<LtResponse<Void>>() {
      });
   }

   @Override
   public LtResponse<Void> abortTrade(UUID sessionId, UUID tradeSessionId) {
      LtRequest r = new LtRequest(Function.ABORT_TRADE);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      r.addQueryParameter(Param.TRADE_SESSION_ID, tradeSessionId.toString());
      return sendRequest(r, new TypeReference<LtResponse<Void>>() {
      });
   }

   @Override
   public LtResponse<Void> sendEncryptedChatMessage(UUID sessionId, EncryptedChatMessageParameters params) {
      LtRequest r = new LtRequest(Function.SEND_CHAT_MESSAGE);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      r.setPostObject(_objectMapper, params);
      return sendRequest(r, new TypeReference<LtResponse<Void>>() {
      });
   }

   @Override
   public LtResponse<TradeSession> waitForTradeSessionChange(UUID sessionId, UUID tradeSessionId,
         long tradeSessionTimestamp) {
      LtRequest r = new LtRequest(Function.WAIT_FOR_TRADE_SESSION_CHANGE);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      r.addQueryParameter(Param.TRADE_SESSION_ID, tradeSessionId.toString());
      r.addQueryParameter(Param.TIMESTAMP, Long.toString(tradeSessionTimestamp));
      return sendRequest(r, new TypeReference<LtResponse<TradeSession>>() {
      });
   }

   @Override
   public LtResponse<Void> stopWaitingForTradeSessionChange(UUID sessionId) {
      LtRequest r = new LtRequest(Function.STOP_WAITING_FOR_TRADE_SESSION_CHANGE);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      return sendRequest(r, new TypeReference<LtResponse<Void>>() {
      });
   }

   @Override
   public LtResponse<Long> waitForTraderChange(Address traderId, UUID token, long traderTimestamp) {
      LtRequest r = new LtRequest(Function.WAIT_FOR_TRADER_CHANGE);
      r.addQueryParameter(Param.TRADER_ID, traderId.toString());
      r.addQueryParameter(Param.TOKEN, token.toString());
      r.addQueryParameter(Param.TIMESTAMP, Long.toString(traderTimestamp));
      return sendRequest(r, new TypeReference<LtResponse<Long>>() {
      });
   }

   @Override
   public LtResponse<Void> stopWaitingForTraderChange(UUID token) {
      LtRequest r = new LtRequest(Function.STOP_WAITING_FOR_TRADER_CHANGE);
      r.addQueryParameter(Param.TOKEN, token.toString());
      return sendRequest(r, new TypeReference<LtResponse<Void>>() {
      });
   }

   @Override
   public LtResponse<Void> setTraderNotificationEmail(UUID sessionId, String email) {
      LtRequest r = new LtRequest(Function.SET_NOTIFICATION_EMAIL);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      r.addQueryParameter(Param.EMAIL, email);
      return sendRequest(r, new TypeReference<LtResponse<Void>>() {
      });
   }

   @Override
   public LtResponse<GeocoderSearchResults> searchGeocoder(UUID sessionId, String query, int maxResults) {
      LtRequest r = new LtRequest(Function.SEARCH_GEOCODER)
              .addQueryParameter(Param.SESSION_ID, sessionId.toString())
              .addQueryParameter(Param.QUERY, query)
              .addQueryParameter(Param.MAX_RESULTS, String.valueOf(maxResults));
      return sendRequest(r, new TypeReference<LtResponse<GeocoderSearchResults>>() {
      });
   }

   @Override
   public LtResponse<GeocoderSearchResults> reverseGeocoder(UUID sessionId, double lat, double lon) {
      LtRequest r = new LtRequest(Function.REVERSE_GEOCODER)
              .addQueryParameter(Param.SESSION_ID, sessionId.toString())
              .addQueryParameter(Param.LATITUDE, String.valueOf(lat))
              .addQueryParameter(Param.LONGITUDE, String.valueOf(lon));
      return sendRequest(r, new TypeReference<LtResponse<GeocoderSearchResults>>() {
      });
   }

   @Override
   public LtResponse<Void> deleteTradeHistory(UUID sessionId, UUID tradeHistory) {
      LtRequest r = new LtRequest(Function.DELETE_TRADE_HISTORY);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      r.addQueryParameter(Param.TRADE_SESSION_ID, tradeHistory.toString());
      return sendRequest(r, new TypeReference<LtResponse<Void>>() {
      });
   }

   @Override
   public LtResponse<Void> deleteAccount(UUID sessionId) {
      LtRequest r = new LtRequest(Function.DELETE_ACCOUNT);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      return sendRequest(r, new TypeReference<LtResponse<Void>>() {
      });
   }

   @Override
   public LtResponse<Void> requestMarketRateRefresh(UUID sessionId, UUID tradeSessionId) {
      LtRequest r = new LtRequest(Function.REQUEST_MARKET_RATE_REFRESH);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      r.addQueryParameter(Param.TRADE_SESSION_ID, tradeSessionId.toString());
      return sendRequest(r, new TypeReference<LtResponse<Void>>() {
      });
   }

   @Override
   public LtResponse<Boolean> releaseBtc(UUID sessionId, ReleaseBtcParameters params) {
      LtRequest r = new LtRequest(Function.REQUEST_RELEASE_BTC);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      r.setPostObject(_objectMapper, params);
      return sendRequest(r, new TypeReference<LtResponse<Boolean>>() {
      });
   }

   @Override
   public LtResponse<TraderInfo> getTraderInfo(UUID sessionId) {
      LtRequest r = new LtRequest(Function.GET_TRADER_INFO);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      return sendRequest(r, new TypeReference<LtResponse<TraderInfo>>() {
      });
   }

   @Override
   public LtResponse<PublicTraderInfo> getPublicTraderInfo(UUID sessionId, Address traderIdentity) {
      LtRequest r = new LtRequest(Function.GET_PUBLIC_TRADER_INFO);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      r.addQueryParameter(Param.TRADER_ID, traderIdentity.toString());
      return sendRequest(r, new TypeReference<LtResponse<PublicTraderInfo>>() {
      });
   }

   @Override
   public LtResponse<Captcha> getCaptcha(UUID sessionId) {
      LtRequest r = new LtRequest(Function.GET_CAPTCHA);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      return sendRequest(r, new TypeReference<LtResponse<Captcha>>() {
      });
   }

   @Override
   public LtResponse<Boolean> solveCaptcha(UUID sessionId, String captchaSolution) {
      LtRequest r = new LtRequest(Function.SOLVE_CAPTCHA);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      r.addQueryParameter(Param.CAPTCHA_SOLUTION, captchaSolution);
      return sendRequest(r, new TypeReference<LtResponse<Boolean>>() {
      });
   }

   @Override
   public LtResponse<Long> getLastTradeSessionChange(Address traderIdentity) {
      LtRequest r = new LtRequest(Function.GET_LAST_TRADE_SESSION_CHANGE);
      r.addQueryParameter(Param.TRADER_ID, traderIdentity.toString());
      return sendRequest(r, new TypeReference<LtResponse<Long>>() {
      });
   }

   @Override
   public LtResponse<BtcSellPrice> assessBtcSellPrice(UUID sessionId, BtcSellPriceParameters params) {
      LtRequest r = new LtRequest(Function.ASSESS_BTC_PRICE);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      r.setPostObject(_objectMapper, params);
      return sendRequest(r, new TypeReference<LtResponse<BtcSellPrice>>() {
      });
   }

   @Override
   public LtResponse<Void> changeTradeSessionPrice(UUID sessionId, TradeChangeParameters params) {
      LtRequest r = new LtRequest(Function.CHANGE_TRADE_SESSION_PRICE);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      r.setPostObject(_objectMapper, params);
      return sendRequest(r, new TypeReference<LtResponse<Void>>() {
      });
   }
}
