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

public interface LtConst {
   String TRADE = "/trade";
   String INFO = "/info";
   String MAIL = "/mail";

   interface Param {
      String API_VERSION = "v";
      String SESSION_ID = "sessionId";
      String NICKNAME = "nickname";
      String ADDRESS = "address";
      String SIG_HASH_SESSION_ID = "sigHashSessionId";
      String LONGITUDE = "longitude";
      String LATITUDE = "latitude";
      String CURRENCY = "currency";
      String MINIMUM_FIAT = "minimumFiat";
      String MAXIMUM_FIAT = "maximumFiat";
      String PRICE_FORMULA_ID = "priceFormulaId";
      String PREMIUM = "premium";
      String DESCRIPTION = "description";
      @Deprecated String SELL_ORDER_ID = "sellOrderId";
      String FIAT_OFFERED = "fiatOffered";
      String BITCOIN_ADDRESS = "bitcoinAddress";
      String TRADER_ID = "traderId";
      String LOCALE = "locale";
      String TRADE_SESSION_ID = "tradeSessionId";
      String BITCOIN_DENOMINATION = "bitcoinDenomination";
      String TIMESTAMP = "timestamp";
      String MESSAGE = "message";
      String RAW_HEX_TRANSACTION = "rawtx";
      String CAPTCHA_SOLUTION = "solution";
      String NAME = "name";
      String LIMIT = "limit";
      String OFFSET = "offset";
      String TOKEN = "token";
      String AD_ID = "adId";
      String EMAIL = "email";
      String QUERY = "query";
      String MAX_RESULTS = "maxResults";
   }

   interface Function {
      String CREATE_TRADER = "createTrader";
      String CREATE_SESSION = "createSession";
      String TRADER_LOGIN = "traderLogin";
      @Deprecated String LIST_SELL_ORDERS = "listSellOrders";
      String GET_SUPPORTED_PRICE_FORMULAS = "getSupportedPriceFormulas";
      @Deprecated String CREATE_SELL_ORDER = "createSellOrder";
      @Deprecated String GET_SELL_ORDER = "getSellOrder";
      @Deprecated String EDIT_SELL_ORDER = "editSellOrder";
      @Deprecated String ACTIVATE_SELL_ORDER = "activateSellOrder";
      @Deprecated String DEACTIVATE_SELL_ORDER = "deactivateSellOrder";
      @Deprecated String DELETE_SELL_ORDER = "deleteSellOrder";
      @Deprecated String CREATE_INSTANT_BUY_ORDER = "createInstantBuyOrder";
      String GET_ACTIVE_TRADE_SESSIONS = "getActiveTradeSessions";
      String GET_FINAL_TRADE_SESSIONS = "getFinalTradeSessions";
      String GET_TRADE_SESSIONS = "getTradeSessions";
      String GET_TRADE_SESSION = "getTradeSession";
      @Deprecated String SELL_ORDER_SEARCH = "sellOrderSearch";
      String ACCEPT_TRADE = "acceptTrade";
      String ABORT_TRADE = "abortTrade";
      String SEND_CHAT_MESSAGE = "sendChatMessage";
      String WAIT_FOR_TRADE_SESSION_CHANGE = "waitForTradeSessionChange";
      String STOP_WAITING_FOR_TRADE_SESSION_CHANGE = "stopWaitingForTradeSessionChange";
      String WAIT_FOR_TRADER_CHANGE = "waitForTraderChange";
      String STOP_WAITING_FOR_TRADER_CHANGE = "stopWaitingForTraderChange";
      String REQUEST_MARKET_RATE_REFRESH = "requestMarketRateRefresh";
      String REQUEST_RELEASE_BTC = "releaseBtc";
      String GET_TRADER_INFO = "getTraderInfo";
      String GET_PUBLIC_TRADER_INFO = "getPublicTraderInfo";
      String GET_CAPTCHA = "getCaptcha";
      String SOLVE_CAPTCHA = "solveCaptcha";
      String GET_LAST_TRADE_SESSION_CHANGE = "getLastTradeSessionChange";
      String ASSESS_BTC_PRICE = "assessBtcPrice";
      String CHANGE_TRADE_SESSION_PRICE = "changeTradeSessionPrice";
      String AD_SEARCH = "adSearch";
      String LIST_ADS = "listAds";
      String CREATE_AD = "createAd";
      String EDIT_AD = "editAd";
      String ACTIVATE_AD = "activateAd";
      String DEACTIVATE_AD = "deactivateAd";
      String GET_AD = "getAd";
      String DELETE_AD = "deleteAd";
      String GET_ACTIVE_ADS = "getActiveAds";
      String CREATE_TRADE = "createTrade";
      String SET_TRADE_RECEIVING_ADDRESS = "setTradeReceivingAddress";
      String SET_NOTIFICATION_EMAIL = "setNotificationEmail";
      String MAIL_LINK_CONFIRM_MAIL = "confirmMail";
      String MAIL_LINK_UNSUBSCRIBE_MAIL = "unsubscribeMail";
      String MAIL_LINK_AD_RENEWAL = "renewAd";
      String LT_API_HEALTHCHECK = "apiHealthCheck";

      String SEARCH_GEOCODER = "searchGeocoder";
      String REVERSE_GEOCODER = "reverseGeocode";
      String DELETE_TRADE_HISTORY = "deleteTradeHistory";
      String DELETE_ACCOUNT = "deleteAccount";
   }
}
