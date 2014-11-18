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

public class LtConst {

   public static final String TRADE = "/trade";
   public static final String INFO = "/info";
   public static final String MAIL = "/mail";

   public static class Param {
      public static final String API_VERSION = "v";
      public static final String SESSION_ID = "sessionId";
      public static final String NICKNAME = "nickname";
      public static final String ADDRESS = "address";
      public static final String SIG_HASH_SESSION_ID = "sigHashSessionId";
      public static final String LONGITUDE = "longitude";
      public static final String LATITUDE = "latitude";
      public static final String CURRENCY = "currency";
      public static final String MINIMUM_FIAT = "minimumFiat";
      public static final String MAXIMUM_FIAT = "maximumFiat";
      public static final String PRICE_FORMULA_ID = "priceFormulaId";
      public static final String PREMIUM = "premium";
      public static final String DESCRIPTION = "description";
      @Deprecated
      public static final String SELL_ORDER_ID = "sellOrderId";
      public static final String FIAT_OFFERED = "fiatOffered";
      public static final String BITCOIN_ADDRESS = "bitcoinAddress";
      public static final String TRADER_ID = "traderId";
      public static final String LOCALE = "locale";
      public static final String TRADE_SESSION_ID = "tradeSessionId";
      public static final String BITCOIN_DENOMINATION = "bitcoinDenomination";
      public static final String TIMESTAMP = "timestamp";
      public static final String MESSAGE = "message";
      public static final String RAW_HEX_TRANSACTION = "rawtx";
      public static final String CAPTCHA_SOLUTION = "solution";
      public static final String NAME = "name";
      public static final String LIMIT = "limit";
      public static final String OFFSET = "offset";
      public static final String TOKEN = "token";
      public static final String AD_ID = "adId";
      public static final String EMAIL = "email";
      public static final String QUERY = "query";
      public static final String MAX_RESULTS = "maxResults";
   }

   public static class Function {

      public static final String CREATE_TRADER = "createTrader";
      public static final String CREATE_SESSION = "createSession";
      public static final String TRADER_LOGIN = "traderLogin";
      @Deprecated
      public static final String LIST_SELL_ORDERS = "listSellOrders";
      public static final String GET_SUPPORTED_PRICE_FORMULAS = "getSupportedPriceFormulas";
      @Deprecated
      public static final String CREATE_SELL_ORDER = "createSellOrder";
      @Deprecated
      public static final String GET_SELL_ORDER = "getSellOrder";
      @Deprecated
      public static final String EDIT_SELL_ORDER = "editSellOrder";
      @Deprecated
      public static final String ACTIVATE_SELL_ORDER = "activateSellOrder";
      @Deprecated
      public static final String DEACTIVATE_SELL_ORDER = "deactivateSellOrder";
      @Deprecated
      public static final String DELETE_SELL_ORDER = "deleteSellOrder";
      @Deprecated
      public static final String CREATE_INSTANT_BUY_ORDER = "createInstantBuyOrder";
      public static final String GET_ACTIVE_TRADE_SESSIONS = "getActiveTradeSessions";
      public static final String GET_FINAL_TRADE_SESSIONS = "getFinalTradeSessions";
      public static final String GET_TRADE_SESSIONS = "getTradeSessions";
      public static final String GET_TRADE_SESSION = "getTradeSession";
      @Deprecated
      public static final String SELL_ORDER_SEARCH = "sellOrderSearch";
      public static final String ACCEPT_TRADE = "acceptTrade";
      public static final String ABORT_TRADE = "abortTrade";
      public static final String SEND_CHAT_MESSAGE = "sendChatMessage";
      public static final String WAIT_FOR_TRADE_SESSION_CHANGE = "waitForTradeSessionChange";
      public static final String STOP_WAITING_FOR_TRADE_SESSION_CHANGE = "stopWaitingForTradeSessionChange";
      public static final String WAIT_FOR_TRADER_CHANGE = "waitForTraderChange";
      public static final String STOP_WAITING_FOR_TRADER_CHANGE = "stopWaitingForTraderChange";
      public static final String REQUEST_MARKET_RATE_REFRESH = "requestMarketRateRefresh";
      public static final String REQUEST_RELEASE_BTC = "releaseBtc";
      public static final String GET_TRADER_INFO = "getTraderInfo";
      public static final String GET_PUBLIC_TRADER_INFO = "getPublicTraderInfo";
      public static final String GET_CAPTCHA = "getCaptcha";
      public static final String SOLVE_CAPTCHA = "solveCaptcha";
      public static final String GET_LAST_TRADE_SESSION_CHANGE = "getLastTradeSessionChange";
      public static final String ASSESS_BTC_PRICE = "assessBtcPrice";
      public static final String CHANGE_TRADE_SESSION_PRICE = "changeTradeSessionPrice";
      public static final String AD_SEARCH = "adSearch";
      public static final String LIST_ADS = "listAds";
      public static final String CREATE_AD = "createAd";
      public static final String EDIT_AD = "editAd";
      public static final String ACTIVATE_AD = "activateAd";
      public static final String DEACTIVATE_AD = "deactivateAd";
      public static final String GET_AD = "getAd";
      public static final String DELETE_AD = "deleteAd";
      public static final String GET_ACTIVE_ADS = "getActiveAds";
      public static final String CREATE_TRADE = "createTrade";
      public static final String SET_TRADE_RECEIVING_ADDRESS = "setTradeReceivingAddress";
      public static final String SET_NOTIFICATION_EMAIL = "setNotificationEmail";
      public static final String MAIL_LINK_CONFIRM_MAIL = "confirmMail";
      public static final String MAIL_LINK_UNSUBSCRIBE_MAIL = "unsubscribeMail";
      public static final String MAIL_LINK_AD_RENEWAL = "renewAd";
      public static final String LT_API_HEALTHCHECK = "apiHealthCheck";

      public static final String SEARCH_GEOCODER = "searchGeocoder";
      public static final String REVERSE_GEOCODER = "reverseGeocode";

   }

}
