package com.mycelium.lt.api;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import com.mrd.bitlib.model.Address;
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
import com.mycelium.lt.api.params.EncryptedChatMessageParameters;
import com.mycelium.lt.api.params.InstantBuyOrderParameters;
import com.mycelium.lt.api.params.LoginParameters;
import com.mycelium.lt.api.params.ReleaseBtcParameters;
import com.mycelium.lt.api.params.SearchParameters;
import com.mycelium.lt.api.params.TradeChangeParameters;
import com.mycelium.lt.api.params.TradeParameters;
import com.mycelium.lt.api.params.TraderParameters;

public interface LtApi {

   public static final int MAXIMUM_TRADER_SELL_ORDERS = 10;
   public static final double MINIMUM_SELL_PREMIUM = -90.0;
   public static final double MAXIMUM_SELL_PREMIUM = 100.0;
   public static final int MAXIMUM_TRADER_NAME_LENGTH = 20;
   public static final int MAXIMUM_SELL_ORDER_DESCRIPTION_LENGTH = 1024 * 10;
   public static final int MAXIMUM_SELL_ORDER_SEARCH_LIMIT = 100;
   public static final int MAX_ENCRYPTED_CHAT_MESSAGE_SIZE = 2048;
   public static final int MAX_LOCATION_NAME_LENGTH = 1024;

   /**
    * The current version of the Local Trader API
    */
   public static final int VERSION = 8;

   public final static int ERROR_CODE_SUCCESS = 0;
   public final static int ERROR_CODE_NO_SERVER_CONNECTION = 1;
   public final static int ERROR_CODE_INCOMPATIBLE_API_VERSION = 2;
   public final static int ERROR_CODE_INTERNAL_CLIENT_ERROR = 3;
   public final static int ERROR_CODE_INVALID_SESSION = 4;
   public final static int ERROR_CODE_CAPTCHA_NOT_SOLVED = 5;
   public final static int ERROR_CODE_INVALID_ARGUMENT = 6;
   public final static int ERROR_CODE_TRADER_LOGIN_REQUIRED = 7;
   public final static int ERROR_CODE_INVALID_STATE = 8;
   public final static int ERROR_CODE_MAXIMUM_SELL_ORDERS_REACHED = 9;
   public final static int ERROR_CODE_INTERNAL_SERVER_ERROR = 10;
   public final static int ERROR_CODE_TRADER_NICKNAME_NOT_UNIQUE = 11;
   public final static int ERROR_CODE_TRADE_SESSION_TIMESTAMP_MISMATCH = 12;
   public final static int ERROR_CODE_TRADER_DOES_NOT_EXIST = 13;
   public final static int ERROR_CODE_TRADER_EXISTS_WITH_ANOTHER_NAME = 14;
   public final static int ERROR_CODE_CANNOT_TRADE_WITH_SELF = 15;
   public static final int ERROR_CODE_WAIT_TIMEOUT = 16;
   public final static int ERROR_CODE_TRADED_AMOUNT_TOO_SMALL = 17;
   public final static int ERROR_CODE_INVALID_FUNDING_TRANSACTION = 18;
   public final static int ERROR_CODE_PRICE_FORMULA_NOT_AVAILABLE = 19;

   public static final String TRADE_ACTIVITY_NOTIFICATION_KEY = "tradeActivity";
   
   
   public static final String TRADE_CREATED_NOTIFICATION_TYPE = "trade_created";
   public static final String TRADE_CHANGED_NOTIFICATION_TYPE = "trade_changed";
   public static final String TRADE_FINAL_NOTIFICATION_TYPE = "trade_final";
   
   public LtResponse<LtSession> createSession(int apiVersion, String locale, String bitcoinDenomination);

   public LtResponse<Captcha> getCaptcha(UUID sessionId);

   public LtResponse<Boolean> solveCaptcha(UUID sessionId, String captchaSolution);

   public LtResponse<Void> createTrader(UUID sessionId, TraderParameters params);

   /**
    * Return the Nickname of the Trader
    * @param sessionId session Id obtained from createSession
    * @param params
    * @return
    */
   public LtResponse<String> traderLogin(UUID sessionId, LoginParameters params);

   public LtResponse<Collection<SellOrder>> listSellOrders(UUID sessionId);

   public LtResponse<List<PriceFormula>> getSupportedPriceFormulas(UUID sessionId);

   public LtResponse<UUID> createSellOrder(UUID sessionId, TradeParameters params);
   
   public LtResponse<SellOrder> getSellOrder(UUID sessionId, UUID sellOrderId);

   public LtResponse<Void> deleteSellOrder(UUID sessionId, UUID sellOrderId);
   
   public LtResponse<Void> activateSellOrder(UUID sessionId, UUID sellOrderId);
   
   public LtResponse<Void> editSellOrder(UUID sessionId, UUID sellOrderId, TradeParameters params);
   
   public LtResponse<Void> deactivateSellOrder(UUID sessionId, UUID sellOrderId);

   public LtResponse<List<SellOrderSearchItem>> sellOrderSearch(UUID sessionId, SearchParameters params);

   public LtResponse<UUID> createInstantBuyOrder(UUID sessionId, InstantBuyOrderParameters params);

   public LtResponse<LinkedList<TradeSession>> getActiveTradeSessions(UUID sessionId);

   public LtResponse<LinkedList<TradeSession>> getFinalTradeSessions(UUID sessionId, int limit, int offset);

   public LtResponse<LinkedList<TradeSession>> getTradeSessions(UUID sessionId, int limit, int offset);

   public LtResponse<TradeSession> getTradeSession(UUID sessionId, UUID tradeSessionId);

   public LtResponse<Void> acceptTrade(UUID sessionId, UUID tradeSessionId, long tradeSessionTimestamp);

   public LtResponse<Void> abortTrade(UUID sessionId, UUID tradeSessionId);

   public LtResponse<Void> sendEncryptedChatMessage(UUID sessionId, EncryptedChatMessageParameters params);

   public LtResponse<Void> requestMarketRateRefresh(UUID sessionId, UUID tradeSessionId);

   public LtResponse<BtcSellPrice> assessBtcSellPrice(UUID sessionId, BtcSellPriceParameters params);

   public LtResponse<Void> changeTradeSessionPrice(UUID sessionId, TradeChangeParameters params);

   public LtResponse<Boolean> releaseBtc(UUID sessionId, ReleaseBtcParameters params);

   public LtResponse<TraderInfo> getTraderInfo(UUID sessionId);
   
   public LtResponse<PublicTraderInfo> getPublicTraderInfo(UUID sessionId, Address traderIdentity);

   /**
    * Get the last change timestamp for a trader identity without authentication
    * or session id
    */
   public LtResponse<Long> getLastTradeSessionChange(Address traderIdentity);

   public LtResponse<TradeSession> waitForTradeSessionChange(UUID sessionId, UUID tradeSessionId,
         long tradeSessionTimestamp);

   public LtResponse<Void> stopWaitingForTradeSessionChange(UUID token);

   public LtResponse<Long> waitForTraderChange(Address traderId, UUID token, long traderTimestamp);

   public LtResponse<Void> stopWaitingForTraderChange(UUID token);
}
