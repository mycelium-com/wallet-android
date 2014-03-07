package com.mycelium.lt.api;

public class LtConst {

   public static final String TRADE = "/trade";

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
   }
   public static class Function {

      public static final String CREATE_TRADER = "createTrader";
      public static final String CREATE_SESSION = "createSession";
      public static final String TRADER_LOGIN = "traderLogin";
      public static final String LIST_SELL_ORDERS = "listSellOrders";
      public static final String GET_SUPPORTED_PRICE_FORMULAS = "getSupportedPriceFormulas";
      public static final String CREATE_SELL_ORDER = "createSellOrder";
      public static final String GET_SELL_ORDER = "getSellOrder";
      public static final String EDIT_SELL_ORDER = "editSellOrder";
      public static final String ACTIVATE_SELL_ORDER = "activateSellOrder";
      public static final String DEACTIVATE_SELL_ORDER = "deactivateSellOrder";
      public static final String DELETE_SELL_ORDER = "deleteSellOrder";
      public static final String CREATE_INSTANT_BUY_ORDER = "createInstantBuyOrder";
      public static final String GET_OPEN_TRADE_SESSIONS = "getOpenTradeSessions";
      public static final String GET_FINAL_TRADE_SESSIONS = "getFinalTradeSessions";
      public static final String GET_TRADE_SESSIONS = "getTradeSessions";
      public static final String GET_TRADE_SESSION = "getTradeSession";
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
      public static final String GET_CAPTCHA = "getCaptcha";
      public static final String SOLVE_CAPTCHA = "solveCaptcha";
      public static final String GET_LAST_TRADE_SESSION_CHANGE = "getLastTradeSessionChange";
      public static final String ASSESS_BTC_PRICE = "assessBtcPrice";
      public static final String CHANGE_TRADE_SESSION_PRICE = "changeTradeSessionPrice";
   }

}
