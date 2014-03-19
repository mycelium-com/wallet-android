package com.mycelium.lt.api;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mrd.bitlib.model.Address;
import com.mycelium.lt.api.LtConst.Function;
import com.mycelium.lt.api.LtConst.Param;
import com.mycelium.lt.api.model.BtcSellPrice;
import com.mycelium.lt.api.model.Captcha;
import com.mycelium.lt.api.model.LtSession;
import com.mycelium.lt.api.model.PriceFormula;
import com.mycelium.lt.api.model.PublicTraderInfo;
import com.mycelium.lt.api.model.SellOrder;
import com.mycelium.lt.api.model.SellOrderSearchItem;
import com.mycelium.lt.api.model.TradeSession;
import com.mycelium.lt.api.model.TraderInfo;
import com.mycelium.lt.api.params.BtcSellPriceParameters;
import com.mycelium.lt.api.params.ChatMessageParameters;
import com.mycelium.lt.api.params.InstantBuyOrderParameters;
import com.mycelium.lt.api.params.LoginParameters;
import com.mycelium.lt.api.params.ReleaseBtcParameters;
import com.mycelium.lt.api.params.SearchParameters;
import com.mycelium.lt.api.params.TradeChangeParameters;
import com.mycelium.lt.api.params.TradeParameters;
import com.mycelium.lt.api.params.TraderParameters;

public class LtApiClient implements LtApi {

   public static final int TIMEOUT_MS = 60000 * 2;

   public interface Logger {
      public void logError(String message, Exception e);

      public void logError(String message);
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

   private HttpEndpoint _httpEndpoint;
   private ObjectMapper _objectMapper;
   private Logger _logger;

   public LtApiClient(HttpEndpoint httpEndpoint, Logger logger) {
      _httpEndpoint = httpEndpoint;
      _objectMapper = new ObjectMapper();
      // We ignore properties that do not map onto the version of the class we
      // deserialize
      _objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      _objectMapper.registerModule(new LtJsonModule());
      _logger = logger;
   }

   private LtResponse<Void> sendRequest(LtRequest request) {
      HttpURLConnection connection = getConnectionAndSendRequest(request);
      if (connection == null) {
         return new LtResponse<Void>(ERROR_CODE_NO_SERVER_CONNECTION, null);
      }
      return new LtResponse<Void>(ERROR_CODE_SUCCESS, null);
   }

   private <T> LtResponse<T> sendRequest(LtRequest request, TypeReference<LtResponse<T>> typeReference) {
      try {
         HttpURLConnection connection = getConnectionAndSendRequest(request);
         if (connection == null) {
            return new LtResponse<T>(ERROR_CODE_NO_SERVER_CONNECTION, null);
         }
         return _objectMapper.readValue(connection.getInputStream(), typeReference);
      } catch (JsonParseException e) {
         logError("sendRequest failed with Json parsing error.", e);
         return new LtResponse<T>(ERROR_CODE_INTERNAL_CLIENT_ERROR, null);
      } catch (JsonMappingException e) {
         logError("sendRequest failed with Json mapping error.", e);
         return new LtResponse<T>(ERROR_CODE_INTERNAL_CLIENT_ERROR, null);
      } catch (IOException e) {
         logError("sendRequest failed IO exception.", e);
         return new LtResponse<T>(ERROR_CODE_INTERNAL_CLIENT_ERROR, null);
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

   private HttpURLConnection getConnectionAndSendRequest(LtRequest request) {
      try {
         HttpURLConnection connection = getHttpConnection(request, TIMEOUT_MS);
         byte[] data = request.getPostBytes();
         connection.setRequestProperty("Content-Length", String.valueOf(data.length));
         connection.setRequestProperty("Content-Type", "application/json");
         connection.getOutputStream().write(data);
         int status = connection.getResponseCode();
         // Check for status code 2XX
         if (status / 100 == 2) {
            return connection;
         }
         logError("Local Trader server request for class " + request.getClass().toString()
               + " returned HTTP status code " + status);
      } catch (IOException e) {
         logError("getConnectionAndSendRequest failed IO exception.");
         // handle below like the all status codes != 200
      }
      // null simply means no server connection
      return null;
   }

   private HttpURLConnection getHttpConnection(LtRequest request, int timeout) throws IOException {
      URL url = request.getUrl();
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      if (request.isHttps()) {
         SslUtils.configureTrustedCertificate(connection, request.getCertificateThumbprint());
      }
      connection.setConnectTimeout(timeout);
      connection.setReadTimeout(timeout);
      connection.setDoInput(true);
      connection.setDoOutput(true);
      return connection;
   }

   @Override
   public LtResponse<LtSession> createSession(int apiVersion, String locale, String bitcoinDenomination) {
      LtRequest r = new LtRequest(_httpEndpoint, Function.CREATE_SESSION);
      r.addQueryParameter(Param.API_VERSION, Integer.toString(apiVersion));
      r.addQueryParameter(Param.LOCALE, locale);
      r.addQueryParameter(Param.BITCOIN_DENOMINATION, bitcoinDenomination);
      return sendRequest(r, new TypeReference<LtResponse<LtSession>>() {
      });
   }

   @Override
   public LtResponse<Void> createTrader(UUID sessionId, TraderParameters params) {
      LtRequest r = new LtRequest(_httpEndpoint, Function.CREATE_TRADER);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      r.setPostObject(_objectMapper, params);
      return sendRequest(r);
   }

   @Override
   public LtResponse<String> traderLogin(UUID sessionId, LoginParameters params) {
      LtRequest r = new LtRequest(_httpEndpoint, Function.TRADER_LOGIN);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      r.setPostObject(_objectMapper, params);
      return sendRequest(r, new TypeReference<LtResponse<String>>() {
      });
   }

   @Override
   public LtResponse<Collection<SellOrder>> listSellOrders(UUID sessionId) {
      LtRequest r = new LtRequest(_httpEndpoint, Function.LIST_SELL_ORDERS);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      return sendRequest(r, new TypeReference<LtResponse<Collection<SellOrder>>>() {
      });
   }

   @Override
   public LtResponse<List<PriceFormula>> getSupportedPriceFormulas(UUID sessionId) {
      LtRequest r = new LtRequest(_httpEndpoint, Function.GET_SUPPORTED_PRICE_FORMULAS);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      return sendRequest(r, new TypeReference<LtResponse<List<PriceFormula>>>() {
      });
   }

   @Override
   public LtResponse<UUID> createSellOrder(UUID sessionId, TradeParameters params) {
      LtRequest r = new LtRequest(_httpEndpoint, Function.CREATE_SELL_ORDER);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      r.setPostObject(_objectMapper, params);
      return sendRequest(r, new TypeReference<LtResponse<UUID>>() {
      });
   }

   @Override
   public LtResponse<SellOrder> getSellOrder(UUID sessionId, UUID sellOrderId) {
      LtRequest r = new LtRequest(_httpEndpoint, Function.GET_SELL_ORDER);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      r.addQueryParameter(Param.SELL_ORDER_ID, sellOrderId.toString());
      return sendRequest(r, new TypeReference<LtResponse<SellOrder>>() {
      });
   }

   @Override
   public LtResponse<Void> editSellOrder(UUID sessionId, UUID sellOrderId, TradeParameters params) {
      LtRequest r = new LtRequest(_httpEndpoint, Function.EDIT_SELL_ORDER);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      r.addQueryParameter(Param.SELL_ORDER_ID, sellOrderId.toString());
      r.setPostObject(_objectMapper, params);
      return sendRequest(r, new TypeReference<LtResponse<Void>>() {
      });
   }


   @Override
   public LtResponse<Void> activateSellOrder(UUID sessionId, UUID sellOrderId) {
      LtRequest r = new LtRequest(_httpEndpoint, Function.ACTIVATE_SELL_ORDER);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      r.addQueryParameter(Param.SELL_ORDER_ID, sellOrderId.toString());
      return sendRequest(r);
   }

   @Override
   public LtResponse<Void> deactivateSellOrder(UUID sessionId, UUID sellOrderId) {
      LtRequest r = new LtRequest(_httpEndpoint, Function.DEACTIVATE_SELL_ORDER);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      r.addQueryParameter(Param.SELL_ORDER_ID, sellOrderId.toString());
      return sendRequest(r);
   }

   @Override
   public LtResponse<Void> deleteSellOrder(UUID sessionId, UUID sellOrderId) {
      LtRequest r = new LtRequest(_httpEndpoint, Function.DELETE_SELL_ORDER);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      r.addQueryParameter(Param.SELL_ORDER_ID, sellOrderId.toString());
      return sendRequest(r);
   }

   @Override
   public LtResponse<List<SellOrderSearchItem>> sellOrderSearch(UUID sessionId, SearchParameters params) {
      LtRequest r = new LtRequest(_httpEndpoint, Function.SELL_ORDER_SEARCH);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      r.setPostObject(_objectMapper, params);
      return sendRequest(r, new TypeReference<LtResponse<List<SellOrderSearchItem>>>() {
      });
   }

   @Override
   public LtResponse<UUID> createInstantBuyOrder(UUID sessionId, InstantBuyOrderParameters params) {
      LtRequest r = new LtRequest(_httpEndpoint, Function.CREATE_INSTANT_BUY_ORDER);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      r.setPostObject(_objectMapper, params);
      return sendRequest(r, new TypeReference<LtResponse<UUID>>() {
      });
   }

   @Override
   public LtResponse<LinkedList<TradeSession>> getActiveTradeSessions(UUID sessionId) {
      LtRequest r = new LtRequest(_httpEndpoint, Function.GET_ACTIVE_TRADE_SESSIONS);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      return sendRequest(r, new TypeReference<LtResponse<LinkedList<TradeSession>>>() {
      });
   }

   @Override
   public LtResponse<LinkedList<TradeSession>> getFinalTradeSessions(UUID sessionId, int limit, int offset) {
      LtRequest r = new LtRequest(_httpEndpoint, Function.GET_FINAL_TRADE_SESSIONS);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      r.addQueryParameter(Param.LIMIT, Integer.toString(limit));
      r.addQueryParameter(Param.OFFSET, Integer.toString(offset));
      return sendRequest(r, new TypeReference<LtResponse<LinkedList<TradeSession>>>() {
      });
   }

   @Override
   public LtResponse<LinkedList<TradeSession>> getTradeSessions(UUID sessionId, int limit, int offset) {
      LtRequest r = new LtRequest(_httpEndpoint, Function.GET_TRADE_SESSIONS);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      r.addQueryParameter(Param.LIMIT, Integer.toString(limit));
      r.addQueryParameter(Param.OFFSET, Integer.toString(offset));
      return sendRequest(r, new TypeReference<LtResponse<LinkedList<TradeSession>>>() {
      });
   }

   @Override
   public LtResponse<TradeSession> getTradeSession(UUID sessionId, UUID tradeSessionId) {
      LtRequest r = new LtRequest(_httpEndpoint, Function.GET_TRADE_SESSION);
      r.addQueryParameter("sessionId", sessionId.toString()).addQueryParameter(Param.TRADE_SESSION_ID,
            tradeSessionId.toString());
      return sendRequest(r, new TypeReference<LtResponse<TradeSession>>() {
      });
   }

   @Override
   public LtResponse<Void> acceptTrade(UUID sessionId, UUID tradeSessionId, long tradeSessionTimestamp) {
      LtRequest r = new LtRequest(_httpEndpoint, Function.ACCEPT_TRADE);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      r.addQueryParameter(Param.TRADE_SESSION_ID, tradeSessionId.toString());
      r.addQueryParameter(Param.TIMESTAMP, Long.toString(tradeSessionTimestamp));
      return sendRequest(r);
   }

   @Override
   public LtResponse<Void> abortTrade(UUID sessionId, UUID tradeSessionId) {
      LtRequest r = new LtRequest(_httpEndpoint, Function.ABORT_TRADE);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      r.addQueryParameter(Param.TRADE_SESSION_ID, tradeSessionId.toString());
      return sendRequest(r);
   }

   @Override
   public LtResponse<Void> sendChatMessage(UUID sessionId, ChatMessageParameters params) {
      LtRequest r = new LtRequest(_httpEndpoint, Function.SEND_CHAT_MESSAGE);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      r.setPostObject(_objectMapper, params);
      return sendRequest(r);
   }

   @Override
   public LtResponse<TradeSession> waitForTradeSessionChange(UUID sessionId, UUID tradeSessionId,
         long tradeSessionTimestamp) {
      LtRequest r = new LtRequest(_httpEndpoint, Function.WAIT_FOR_TRADE_SESSION_CHANGE);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      r.addQueryParameter(Param.TRADE_SESSION_ID, tradeSessionId.toString());
      r.addQueryParameter(Param.TIMESTAMP, Long.toString(tradeSessionTimestamp));
      return sendRequest(r, new TypeReference<LtResponse<TradeSession>>() {
      });
   }

   @Override
   public LtResponse<Void> stopWaitingForTradeSessionChange(UUID sessionId) {
      LtRequest r = new LtRequest(_httpEndpoint, Function.STOP_WAITING_FOR_TRADE_SESSION_CHANGE);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      return sendRequest(r, new TypeReference<LtResponse<Void>>() {
      });
   }

   @Override
   public LtResponse<Long> waitForTraderChange(Address traderId, UUID token, long traderTimestamp) {
      LtRequest r = new LtRequest(_httpEndpoint, Function.WAIT_FOR_TRADER_CHANGE);
      r.addQueryParameter(Param.TRADER_ID, traderId.toString());
      r.addQueryParameter(Param.TOKEN, token.toString());
      r.addQueryParameter(Param.TIMESTAMP, Long.toString(traderTimestamp));
      return sendRequest(r, new TypeReference<LtResponse<Long>>() {
      });
   }

   @Override
   public LtResponse<Void> stopWaitingForTraderChange(UUID token) {
      LtRequest r = new LtRequest(_httpEndpoint, Function.STOP_WAITING_FOR_TRADER_CHANGE);
      r.addQueryParameter(Param.TOKEN, token.toString());
      return sendRequest(r, new TypeReference<LtResponse<Void>>() {
      });
   }

   @Override
   public LtResponse<Void> requestMarketRateRefresh(UUID sessionId, UUID tradeSessionId) {
      LtRequest r = new LtRequest(_httpEndpoint, Function.REQUEST_MARKET_RATE_REFRESH);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      r.addQueryParameter(Param.TRADE_SESSION_ID, tradeSessionId.toString());
      return sendRequest(r);
   }

   @Override
   public LtResponse<Boolean> releaseBtc(UUID sessionId, ReleaseBtcParameters params) {
      LtRequest r = new LtRequest(_httpEndpoint, Function.REQUEST_RELEASE_BTC);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      r.setPostObject(_objectMapper, params);
      return sendRequest(r, new TypeReference<LtResponse<Boolean>>() {
      });
   }

   @Override
   public LtResponse<TraderInfo> getTraderInfo(UUID sessionId) {
      LtRequest r = new LtRequest(_httpEndpoint, Function.GET_TRADER_INFO);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      return sendRequest(r, new TypeReference<LtResponse<TraderInfo>>() {
      });
   }
   
   @Override
   public LtResponse<PublicTraderInfo> getPublicTraderInfo(UUID sessionId, Address traderIdentity) {
      LtRequest r = new LtRequest(_httpEndpoint, Function.GET_PUBLIC_TRADER_INFO);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      r.addQueryParameter(Param.TRADER_ID, traderIdentity.toString());
      return sendRequest(r, new TypeReference<LtResponse<PublicTraderInfo>>() {
      });
   }


   @Override
   public LtResponse<Captcha> getCaptcha(UUID sessionId) {
      LtRequest r = new LtRequest(_httpEndpoint, Function.GET_CAPTCHA);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      return sendRequest(r, new TypeReference<LtResponse<Captcha>>() {
      });
   }

   @Override
   public LtResponse<Boolean> solveCaptcha(UUID sessionId, String captchaSolution) {
      LtRequest r = new LtRequest(_httpEndpoint, Function.SOLVE_CAPTCHA);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      r.addQueryParameter(Param.CAPTCHA_SOLUTION, captchaSolution);
      return sendRequest(r, new TypeReference<LtResponse<Boolean>>() {
      });
   }

   @Override
   public LtResponse<Long> getLastTradeSessionChange(Address traderIdentity) {
      LtRequest r = new LtRequest(_httpEndpoint, Function.GET_LAST_TRADE_SESSION_CHANGE);
      r.addQueryParameter(Param.TRADER_ID, traderIdentity.toString());
      return sendRequest(r, new TypeReference<LtResponse<Long>>() {
      });
   }

   @Override
   public LtResponse<BtcSellPrice> assessBtcSellPrice(UUID sessionId, BtcSellPriceParameters params) {
      LtRequest r = new LtRequest(_httpEndpoint, Function.ASSESS_BTC_PRICE);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      r.setPostObject(_objectMapper, params);
      return sendRequest(r, new TypeReference<LtResponse<BtcSellPrice>>() {
      });
   }

   @Override
   public LtResponse<Void> changeTradeSessionPrice(UUID sessionId, TradeChangeParameters params) {
      LtRequest r = new LtRequest(_httpEndpoint, Function.CHANGE_TRADE_SESSION_PRICE);
      r.addQueryParameter(Param.SESSION_ID, sessionId.toString());
      r.setPostObject(_objectMapper, params);
      return sendRequest(r, new TypeReference<LtResponse<Void>>() {
      });
   }

}
