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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import com.mrd.bitlib.model.Address;
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

public interface LtApi {
   int MAXIMUM_TRADER_ADS = 10;
   double MINIMUM_SELL_PREMIUM = -90.0;
   double MAXIMUM_SELL_PREMIUM = 100.0;
   int MAXIMUM_TRADER_NAME_LENGTH = 20;
   int MAXIMUM_AD_DESCRIPTION_LENGTH = 1024 * 10;
   int MAXIMUM_AD_SEARCH_LIMIT = 100;
   int MAX_ENCRYPTED_CHAT_MESSAGE_SIZE = 2048;
   int MAX_LOCATION_NAME_LENGTH = 1024;

   /**
    * The current version of the Local Trader API
    */
   int VERSION = 9;

   int ERROR_CODE_SUCCESS = 0;
   int ERROR_CODE_NO_SERVER_CONNECTION = 1;
   int ERROR_CODE_INCOMPATIBLE_API_VERSION = 2;
   int ERROR_CODE_INTERNAL_CLIENT_ERROR = 3;
   int ERROR_CODE_INVALID_SESSION = 4;
   int ERROR_CODE_CAPTCHA_NOT_SOLVED = 5;
   int ERROR_CODE_INVALID_ARGUMENT = 6;
   int ERROR_CODE_TRADER_LOGIN_REQUIRED = 7;
   int ERROR_CODE_INVALID_STATE = 8;
   int ERROR_CODE_MAXIMUM_ADS_REACHED = 9;
   int ERROR_CODE_INTERNAL_SERVER_ERROR = 10;
   int ERROR_CODE_TRADER_NICKNAME_NOT_UNIQUE = 11;
   int ERROR_CODE_TRADE_SESSION_TIMESTAMP_MISMATCH = 12;
   int ERROR_CODE_TRADER_DOES_NOT_EXIST = 13;
   int ERROR_CODE_TRADER_EXISTS_WITH_ANOTHER_NAME = 14;
   int ERROR_CODE_CANNOT_TRADE_WITH_SELF = 15;
   int ERROR_CODE_WAIT_TIMEOUT = 16;
   int ERROR_CODE_TRADED_AMOUNT_TOO_SMALL = 17;
   int ERROR_CODE_INVALID_FUNDING_TRANSACTION = 18;
   int ERROR_CODE_PRICE_FORMULA_NOT_AVAILABLE = 19;
   int ERROR_CODE_WRONG_TRADER = 20;

   String TRADE_ACTIVITY_NOTIFICATION_KEY = "tradeActivity";
   String AD_ACTIVITY_NOTIFICATION_KEY = "adActivity";

   String TRADE_CREATED_NOTIFICATION_TYPE = "trade_created";
   String TRADE_CHANGED_NOTIFICATION_TYPE = "trade_changed";
   String TRADE_FINAL_NOTIFICATION_TYPE = "trade_final";
   String AD_TIME_OUT_NOTIFICATION_TYPE = "ad_time_out";

   /**
    * Example HTTP POST:
    * curl  -k -X POST -H "Content-Type: application/json" "https://node3.mycelium.com/lttestnet/createSession?v=9&locale=de&bitcoinDenomination=mBTC"
    */
   LtResponse<LtSession> createSession(int apiVersion, String locale, String bitcoinDenomination);

   /*
   Example HTTP POST:
   curl  -k -X POST -H "Content-Type: application/json" https://node3.mycelium.com/lttestnet/getCaptcha?sessionId=1ecc892f-249f-4e3a-bde8-cb06bb3cf891
    */
   LtResponse<Captcha> getCaptcha(UUID sessionId);

   /*
   Example HTTP POST:
   curl  -k -X POST -H "Content-Type: application/json" https://node3.mycelium.com/lttestnet/solveCaptcha?sessionId=1ecc892f-249f-4e3a-bde8-cb06bb3cf891&solution=65966
    */
   LtResponse<Boolean> solveCaptcha(UUID sessionId, String captchaSolution);

   /*
   Example HTTP POST:
   curl  -k -X POST -H "Content-Type: application/json"
         -d '{"nickname":"Jan","address":"mispsmBU3CWL1uVAb12NvyEzwiTYfQyy3f","publicKey":"020509fbed4d484207997982cb2654834c6e4177024b89aa26dd3df2227906deb4","sigSessionId":"IF3II8BhDOBxT271CV0IgJay07N0V2N/nb4pd5H3DihVLmnEmxEVSRRnntgLfqoDi9UfAe5o1Dh3fVX7iiNMz/U="}'
         https://node3.mycelium.com/lttestnet/createTrader?sessionId=59110bc6-58b7-40c9-b1ef-8279379a4aad
    */
   LtResponse<Void> createTrader(UUID sessionId, TraderParameters params);

   /**
    * Return the Nickname of the Trader
    *
    * @param sessionId
    *           session Id obtained from createSession
    * @return
    * Example HTTP POST:
    * curl  -k -X POST -H "Content-Type: application/json"
    *       -d '{"address":"mispsmBU3CWL1uVAb12NvyEzwiTYfQyy3f","signature":"IEmxnw2URW4n6QgpCkADRk9oU2aMnDwkQ4DJDV+xy0RuD9u0mlBC40wFVsKfVTBV6sJZ8crkNu9PSF6HJBmOiBc=","gcmId":null,"lastTradeSessionChange":0}'
    *       https://node3.mycelium.com/lttestnet/traderLogin?sessionId=59110bc6-58b7-40c9-b1ef-8279379a4aad
    */
   LtResponse<String> traderLogin(UUID sessionId, LoginParameters params);

   /*
   Example HTTP POST:
   curl  -k -X POST -H "Content-Type: application/json" https://node3.mycelium.com/lttestnet/listAds?sessionId=1ecc892f-249f-4e3a-bde8-cb06bb3cf891
    */
   LtResponse<Collection<Ad>> listAds(UUID sessionId);

   LtResponse<List<PriceFormula>> getSupportedPriceFormulas(UUID sessionId);

   /* Example HTTP POST:
   curl  -k -X POST -H "Content-Type: application/json"
          -d '{"type":"SELL_BTC","location":{"name":"Aarhus, Dänemark","longitude":10.2039213180542,"latitude":56.16293716430664,"countryCode":"DK"},"currency":"USD","minimumFiat":10,"maximumFiat":15,"priceFormulaId":"BITSTAMP","premium":5.0,"description":""}'
          https://node3.mycelium.com/lttestnet/createAd?sessionId=1b28a900-61d8-4aac-85b1-943d7b01f241
   */
   LtResponse<UUID> createAd(UUID sessionId, AdParameters params);

   @Deprecated
   LtResponse<SellOrder> getSellOrder(UUID sessionId, UUID sellOrderId);

   LtResponse<Ad> getAd(UUID sessionId, UUID adId);

   @Deprecated
   LtResponse<Void> deleteSellOrder(UUID sessionId, UUID sellOrderId);

   LtResponse<Void> deleteAd(UUID sessionId, UUID adId);

   LtResponse<Void> activateAd(UUID sessionId, UUID adId);

   @Deprecated
   LtResponse<Void> deactivateSellOrder(UUID sessionId, UUID sellOrderId);

   LtResponse<Void> deactivateAd(UUID sessionId, UUID adId);

   LtResponse<Void> editAd(UUID sessionId, UUID adId, AdParameters params);

   @Deprecated
   LtResponse<List<SellOrderSearchItem>> sellOrderSearch(UUID sessionId, SearchParameters params);

   /**
    *
    * @param sessionId the id obtained via a prvious call of create session
    * @param params the params specifying for which ads to search
    * @return list of matching AdSearchItems
    *
    * Example HTTP POST:
    * curl  -k -X POST -H "Content-Type: application/json"
    *       -d '{"location":{"name":"Penzing, Vienna","longitude":16.248472213745117,"latitude":48.216285705566406,"countryCode":"Penzing, Vienna"},"limit":30,"type":"SELL_BTC"}'
    *       https://node3.mycelium.com/lttestnet/adSearch?sessionId=07a7d2cc-61ca-4b78-aa31-1f77f938edd3
    */
   LtResponse<List<AdSearchItem>> adSearch(UUID sessionId, SearchParameters params);

   LtResponse<List<AdSearchItem>> getActiveAds();

   LtResponse<UUID> createInstantBuyOrder(UUID sessionId, InstantBuyOrderParameters params);

   /*
   Example HTTP POST:
   curl  -k -X POST -H "Content-Type: application/json"
         -d '{"adId":"2efdf8f1-6ede-44a6-969c-84c8d3f28d6b","fiatOffered":10}'
         https://node3.mycelium.com/lttestnet/createTrade?sessionId=1ecc892f-249f-4e3a-bde8-cb06bb3cf891
    */
   LtResponse<UUID> createTrade(UUID sessionId, CreateTradeParameters params);

   LtResponse<LinkedList<TradeSession>> getActiveTradeSessions(UUID sessionId);

   /*
   Example HTTP POST:
   curl  -k -X POST -H "Content-Type: application/json" https://node3.mycelium.com/lttestnet/getFinalTradeSessions?sessionId=1ecc892f-249f-4e3a-bde8-cb06bb3cf891&limit=10&offset=0
    */
   LtResponse<LinkedList<TradeSession>> getFinalTradeSessions(UUID sessionId, int limit, int offset);

   LtResponse<LinkedList<TradeSession>> getTradeSessions(UUID sessionId, int limit, int offset);

   /*
   Example HTTP POST:
   curl  -k -X POST -H "Content-Type: application/json" https://node3.mycelium.com/lttestnet/getTradeSession?sessionId=1ecc892f-249f-4e3a-bde8-cb06bb3cf891&tradeSessionId=d4bd5ae2-cf46-413d-87a1-943800da97aa
    */
   LtResponse<TradeSession> getTradeSession(UUID sessionId, UUID tradeSessionId);

   /*
   Example HTTP POST:
   curl  -k -X POST -H "Content-Type: application/json"
         -d '{"tradeSessionId":"d4bd5ae2-cf46-413d-87a1-943800da97aa","address":"mmPDZpf4evpHXkqoqFcuGEPkqng1E79r3g"}'
         https://node3.mycelium.com/lttestnet/setTradeReceivingAddress?sessionId=1ecc892f-249f-4e3a-bde8-cb06bb3cf891
    */
   LtResponse<Void> setTradeReceivingAddress(UUID sessionId, SetTradeReceivingAddressParameters params);

   /*
   Example HTTP POST:
   curl  -k -X POST -H "Content-Type: application/json" https://node3.mycelium.com/lttestnet/acceptTrade?sessionId=1ecc892f-249f-4e3a-bde8-cb06bb3cf891&tradeSessionId=7162cb84-eafc-4878-8e41-71a0da4b7eaa&timestamp=1411038853491
    */
   LtResponse<Void> acceptTrade(UUID sessionId, UUID tradeSessionId, long tradeSessionTimestamp);

   /*
   Example HTTP POST:
   curl  -k -X POST -H "Content-Type: application/json" https://node3.mycelium.com/lttestnet/abortTrade?sessionId=1ecc892f-249f-4e3a-bde8-cb06bb3cf891&tradeSessionId=d4bd5ae2-cf46-413d-87a1-943800da97aa
    */
   LtResponse<Void> abortTrade(UUID sessionId, UUID tradeSessionId);

   /*
   Example HTTP POST:
   curl  -k -X POST -H "Content-Type: application/json"
         -d '{"tradeSessionId":"ecbc6352-6674-4737-9fc3-e354057f4c95","encryptedMessage":"ve2N4HV7PTSbOiN6XGE3khJmcYmqSxzuhBudd5E+HTs"}'
         https://node3.mycelium.com/lttestnet/sendChatMessage?sessionId=a9bb2982-704a-4cba-837e-9015315a6257
    */
   LtResponse<Void> sendEncryptedChatMessage(UUID sessionId, EncryptedChatMessageParameters params);

   /*
   Example HTTP POST:
   curl  -k -X POST -H "Content-Type: application/json" https://node3.mycelium.com/lttestnet/requestMarketRateRefresh?sessionId=1ecc892f-249f-4e3a-bde8-cb06bb3cf891&tradeSessionId=cc39c7a0-50ed-4fb8-bac6-e692f06dcf54
    */
   LtResponse<Void> requestMarketRateRefresh(UUID sessionId, UUID tradeSessionId);

   /*
   Example HTTP POST:
   curl  -k -X POST -H "Content-Type: application/json"
         -d '{"ownerId":"mivtBy3ahEJTekcjRvXR2WDe6BRUtFYUpD","peerId":"mispsmBU3CWL1uVAb12NvyEzwiTYfQyy3f","currency":"USD","fiatTraded":17,"priceFormulaId":"BITSTAMP","premium":5.2}'
         https://node3.mycelium.com/lttestnet/assessBtcPrice?sessionId=1ecc892f-249f-4e3a-bde8-cb06bb3cf891
    */
   LtResponse<BtcSellPrice> assessBtcSellPrice(UUID sessionId, BtcSellPriceParameters params);

   LtResponse<Void> changeTradeSessionPrice(UUID sessionId, TradeChangeParameters params);

   /*
   Example HTTP POST:
   curl  -k -X POST -H "Content-Type: application/json"
         -d '{"tradeSessionId":"0b49935a-1394-4eab-83ca-cb41fdebdcb1","rawHexTransaction":"01000000014620575c9612bd17c03c22b509647fc41587fd7fe60cc5390fe8a43f1ccf248b000000006b483045022100e3166c7cd39f42e15eaf88659cde64d489850cd96a7ca600dcc884af357776cf022075fc4f9879d6386dfd2d21d7d071d3bf1699751eee107c8d05bf3d4f643b4a460121021519a76942ec355e7d65ca4232701660a86c12b38cf22dba073fc268099c5c59ffffffff03e8ca0f00000000001976a9143b3d2e9b59ba5332fb54967cfa6bbb4ad60bd4fe88ac2c7b2000000000001976a9144d290d204a913bc6cfa178eccbb5684454f86f0288ac64210000000000001976a91443dc321b6600511fe0a96a97c2593a90542974d688ac00000000"}'
         https://node3.mycelium.com/lttestnet/releaseBtc?sessionId=a9bb2982-704a-4cba-837e-9015315a6257
    */
   LtResponse<Boolean> releaseBtc(UUID sessionId, ReleaseBtcParameters params);

   /*
   Example HTTP POST:
   curl  -k -X POST -H "Content-Type: application/json" https://node3.mycelium.com/lttestnet/getTraderInfo?sessionId=1ecc892f-249f-4e3a-bde8-cb06bb3cf891
    */
   LtResponse<TraderInfo> getTraderInfo(UUID sessionId);

   /*
   Example HTTP POST:
   curl  -k -X POST -H "Content-Type: application/json" https://node3.mycelium.com/lttestnet/getPublicTraderInfo?sessionId=1ecc892f-249f-4e3a-bde8-cb06bb3cf891&traderId=mivtBy3ahEJTekcjRvXR2WDe6BRUtFYUpD
    */
   LtResponse<PublicTraderInfo> getPublicTraderInfo(UUID sessionId, Address traderIdentity);

   /*
    Get the last change timestamp for a trader identity without authentication
    or session id
    */
   LtResponse<Long> getLastTradeSessionChange(Address traderIdentity);

   /*
   Example HTTP POST:
   curl  -k -X POST -H "Content-Type: application/json" https://node3.mycelium.com/lttestnet/waitForTradeSessionChange?sessionId=1ecc892f-249f-4e3a-bde8-cb06bb3cf891&tradeSessionId=d4bd5ae2-cf46-413d-87a1-943800da97aa&timestamp=1411038153102
    */
   LtResponse<TradeSession> waitForTradeSessionChange(UUID sessionId, UUID tradeSessionId, long tradeSessionTimestamp);

   /*
   Example HTTP POST:
   curl  -k -X POST -H "Content-Type: application/json" https://node3.mycelium.com/lttestnet/stopWaitingForTradeSessionChange?sessionId=1ecc892f-249f-4e3a-bde8-cb06bb3cf891
    */
   LtResponse<Void> stopWaitingForTradeSessionChange(UUID token);

   /*
   Example HTTP POST:
   curl  -k -X POST -H "Content-Type: application/json" https://node3.mycelium.com/lttestnet/waitForTraderChange?traderId=mispsmBU3CWL1uVAb12NvyEzwiTYfQyy3f&token=d68a13cc-0f63-43d8-a152-e196778e8026&timestamp=0
    */
   LtResponse<Long> waitForTraderChange(Address traderId, UUID token, long traderTimestamp);

   /*
   Example HTTP POST:
   curl  -k -X POST -H "Content-Type: application/json" https://node3.mycelium.com/lttestnet/stopWaitingForTraderChange?token=31a0ff0b-949a-4748-9a07-7b58dae45abd
    */
   LtResponse<Void> stopWaitingForTraderChange(UUID token);

   LtResponse<Void> setTraderNotificationEmail(UUID sessionId, String email);

   /*
    Query the geocoder from the backend - only use it as fallback if the geocoder is
    not available on client side

   curl  -k -X POST -H "Content-Type: application/json" "https://node3.mycelium.com/lttestnet/searchGeocoder?sessionId=1ecc892f-249f-4e3a-bde8-cb06bb3cf891&query=Wien"
    */
   LtResponse<GeocoderSearchResults> searchGeocoder(UUID sessionId, String query, int maxResults);

   /*
   Reverse geocode (coord->name) from the backend - only use it as fallback if the geocoder is
   not available on client side

   Example HTTP POST:
   curl  -k -X POST -H "Content-Type: application/json" "https://node3.mycelium.com/lttestnet/reverseGeocode?sessionId=1ecc892f-249f-4e3a-bde8-cb06bb3cf891&latitude=48.1182699&longitude=16.1826199"
    */
   LtResponse<GeocoderSearchResults> reverseGeocoder(UUID sessionId, double lat, double lon);

   LtResponse<Void> deleteTradeHistory(UUID sessionId, UUID tradeHistory);

   LtResponse<Void> deleteAccount(UUID sessionId);
}