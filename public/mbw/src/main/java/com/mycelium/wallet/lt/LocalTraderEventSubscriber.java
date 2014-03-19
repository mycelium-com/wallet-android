package com.mycelium.wallet.lt;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import android.os.Handler;

import com.mycelium.lt.api.model.BtcSellPrice;
import com.mycelium.lt.api.model.Captcha;
import com.mycelium.lt.api.model.PriceFormula;
import com.mycelium.lt.api.model.PublicTraderInfo;
import com.mycelium.lt.api.model.SellOrder;
import com.mycelium.lt.api.model.SellOrderSearchItem;
import com.mycelium.lt.api.model.TradeSession;
import com.mycelium.lt.api.model.TraderInfo;
import com.mycelium.wallet.lt.api.AbortTrade;
import com.mycelium.wallet.lt.api.AcceptTrade;
import com.mycelium.wallet.lt.api.ActivateSellOrder;
import com.mycelium.wallet.lt.api.AssessBtcSellPrice;
import com.mycelium.wallet.lt.api.ChangeTradeSessionPrice;
import com.mycelium.wallet.lt.api.CreateInstantBuyOrder;
import com.mycelium.wallet.lt.api.CreateSellOrder;
import com.mycelium.wallet.lt.api.CreateTrader;
import com.mycelium.wallet.lt.api.DeactivateSellOrder;
import com.mycelium.wallet.lt.api.DeleteSellOrder;
import com.mycelium.wallet.lt.api.EditSellOrder;
import com.mycelium.wallet.lt.api.GetCaptcha;
import com.mycelium.wallet.lt.api.GetFinalTradeSessions;
import com.mycelium.wallet.lt.api.GetOpenTradeSessions;
import com.mycelium.wallet.lt.api.GetPriceFormulas;
import com.mycelium.wallet.lt.api.GetPublicTraderInfo;
import com.mycelium.wallet.lt.api.GetSellOrder;
import com.mycelium.wallet.lt.api.GetSellOrders;
import com.mycelium.wallet.lt.api.GetTradeSession;
import com.mycelium.wallet.lt.api.GetTraderInfo;
import com.mycelium.wallet.lt.api.ReleaseBtc;
import com.mycelium.wallet.lt.api.Request;
import com.mycelium.wallet.lt.api.RequestMarketRateRefresh;
import com.mycelium.wallet.lt.api.SellOrderSearch;
import com.mycelium.wallet.lt.api.SendChatMessage;
import com.mycelium.wallet.lt.api.SolveCaptcha;
import com.mycelium.wallet.lt.api.TryLogin;

public abstract class LocalTraderEventSubscriber {

   private final Handler _handler;

   public LocalTraderEventSubscriber(Handler handler) {
      _handler = handler;
   }

   public Handler getHandler() {
      return _handler;
   }

   /**
    * Called when no connection to Local Trader could be established. If not
    * implemented it will default to onLtError
    */
   public boolean onNoLtConnection() {
      return false;
   }

   /**
    * Called when the Local Trader API version implemented by the server is
    * incompatible with the client.
    */
   public boolean onLtNoIncompatibleVersion() {
      return false;
   }

   /**
    * Called when the Local Trader account does not exist. If not implemented it
    * will default to onLtError
    */
   public boolean onLtNoTraderAccount() {
      return false;
   }

   /**
    * Called if a Local Trader API error occurred which was not handled
    */
   public abstract void onLtError(int errorCode);

   /**
    * Called when it has been discovered that the trader has changed. This
    * indicates that new data is available when getting TraderInfo
    */
   public void onLtTraderActicityNotification() {
   }

   /**
    * Called when a request is about to be sent.
    * 
    * @param request
    *           the request being sent
    */
   public void onLtSendingRequest(Request request) {
   }

   /**
    * Generic method which is called when an API call succeeds and the specific
    * callback has not been implemented
    */
   public void onLtGenericSuccess(Request request) {
   }

   /**
    * Called on successful Local Trader login
    * 
    * @param nickname
    */
   public void onLtLogin(String nickname, TryLogin request) {
      onLtGenericSuccess(request);
   }

   /**
    * Called when the local trade session list has been updated
    */
   public void onLtOpenTradeSessionsFetched(List<TradeSession> list, GetOpenTradeSessions request) {
      onLtGenericSuccess(request);
   }

   /**
    * Called when the a local trade session has been updated
    */
   public void onLtTradeSessionFetched(TradeSession tradeSession, GetTradeSession request) {
      onLtGenericSuccess(request);
   }

   /**
    * Called when the sell orders have been fetched
    */
   public void onLtSellOrdersFetched(Collection<SellOrder> sellOrders, GetSellOrders request) {
      onLtGenericSuccess(request);
   }

   /**
    * Called when price formulas have been fetched
    */
   public void onLtPriceFormulasFetched(List<PriceFormula> priceFormulas, GetPriceFormulas request) {
      onLtGenericSuccess(request);
   }

   /**
    * Called when a sell order has been created
    */
   public void onLtSellOrderCreated(UUID sellOrderId, CreateSellOrder request) {
      onLtGenericSuccess(request);
   }

   public void onLtTraderCreated(CreateTrader request) {
      onLtGenericSuccess(request);
   }

   public void onLtSellOrderSearch(List<SellOrderSearchItem> result, SellOrderSearch request) {
      onLtGenericSuccess(request);
   }

   public void onLtInstantBuyOrderCreated(UUID result, CreateInstantBuyOrder request) {
      onLtGenericSuccess(request);
   }

   public void onLtTradeSessionAccepted(UUID tradeSessionId, AcceptTrade request) {
      onLtGenericSuccess(request);
   }

   public void onLtTradeSessionAborted(UUID tradeSessionId, AbortTrade request) {
      onLtGenericSuccess(request);
   }

   public void onLtChatMessageSent(SendChatMessage request) {
      onLtGenericSuccess(request);
   }

   public void onLtMarketRateRefreshed(RequestMarketRateRefresh request) {
      onLtGenericSuccess(request);
   }

   public void onLtBtcReleased(Boolean success, ReleaseBtc request) {
      onLtGenericSuccess(request);
   }

   public void onLtSellOrderDeleted(UUID sellOrderId, DeleteSellOrder request) {
      onLtGenericSuccess(request);
   }

   public void onLtTradeSessionPriceChanged(UUID tradeSessionId, ChangeTradeSessionPrice request) {
      onLtGenericSuccess(request);
   }

   public void onLtBtcSellPriceAssesed(BtcSellPrice btcSellPrice, AssessBtcSellPrice request) {
      onLtGenericSuccess(request);
   }

   public void onLtFinalTradeSessionsFetched(List<TradeSession> list, GetFinalTradeSessions request) {
      onLtGenericSuccess(request);
   }

   public void onLtCaptchaFetched(Captcha captcha, GetCaptcha request) {
      onLtGenericSuccess(request);
   }

   public void onLtCaptchaSolved(boolean result, SolveCaptcha request) {
      onLtGenericSuccess(request);
   }

   public void onLtTraderInfoFetched(TraderInfo info, GetTraderInfo request) {
      onLtGenericSuccess(request);
   }

   public void onLtSellOrderActivated(UUID _sellOrderId, ActivateSellOrder request) {
      onLtGenericSuccess(request);
   }

   public void onLtSellOrderDeactivated(UUID _sellOrderId, DeactivateSellOrder request) {
      onLtGenericSuccess(request);
   }

   public void onLtSellOrderEdited(EditSellOrder request) {
      onLtGenericSuccess(request);
   }

   public void onLtSellOrderRetrieved(SellOrder sellOrder, GetSellOrder request) {
      onLtGenericSuccess(request);
   }

   public void onLtPublicTraderInfoFetched(PublicTraderInfo info, GetPublicTraderInfo request) {
      onLtGenericSuccess(request);
   }

}
