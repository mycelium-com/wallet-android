package com.mycelium.wallet.lt;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import android.os.Handler;

import com.mycelium.lt.api.model.Ad;
import com.mycelium.lt.api.model.AdSearchItem;
import com.mycelium.lt.api.model.BtcSellPrice;
import com.mycelium.lt.api.model.Captcha;
import com.mycelium.lt.api.model.PriceFormula;
import com.mycelium.lt.api.model.PublicTraderInfo;
import com.mycelium.lt.api.model.TradeSession;
import com.mycelium.lt.api.model.TraderInfo;
import com.mycelium.wallet.lt.api.*;

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
   public void onLtTraderActicityNotification(long timestamp) {
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
    * Called when the ads have been fetched
    */
   public void onLtAdsFetched(Collection<Ad> ads, GetAds request) {
      onLtGenericSuccess(request);
   }

   /**
    * Called when price formulas have been fetched
    */
   public void onLtPriceFormulasFetched(List<PriceFormula> priceFormulas, GetPriceFormulas request) {
      onLtGenericSuccess(request);
   }

   /**
    * Called when an ad has been created
    */
   public void onLtAdCreated(UUID adId, CreateAd request) {
      onLtGenericSuccess(request);
   }

   public void onLtTraderCreated(CreateTrader request) {
      onLtGenericSuccess(request);
   }

   public void onLtAdSearch(List<AdSearchItem> result, AdSearch request) {
      onLtGenericSuccess(request);
   }

   public void onLtTradeCreated(UUID result, CreateTrade request) {
      onLtGenericSuccess(request);
   }

   public void onLtTradeSessionAccepted(UUID tradeSessionId, AcceptTrade request) {
      onLtGenericSuccess(request);
   }

   public void onLtTradeSessionAborted(UUID tradeSessionId, AbortTrade request) {
      onLtGenericSuccess(request);
   }

   public void onLtEncryptedChatMessageSent(SendEncryptedChatMessage request) {
      onLtGenericSuccess(request);
   }

   public void onLtMarketRateRefreshed(RequestMarketRateRefresh request) {
      onLtGenericSuccess(request);
   }

   public void onLtBtcReleased(Boolean success, ReleaseBtc request) {
      onLtGenericSuccess(request);
   }

   public void onLtAdDeleted(UUID adId, DeleteAd request) {
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

   public void onLtAdActivated(UUID adId, ActivateAd request) {
      onLtGenericSuccess(request);
   }

   public void onLtAdDeactivated(UUID adId, DeactivateAd request) {
      onLtGenericSuccess(request);
   }

   public void onLtAdEdited(EditAd request) {
      onLtGenericSuccess(request);
   }

   public void onLtAdRetrieved(Ad ad, GetAd request) {
      onLtGenericSuccess(request);
   }

   public void onLtPublicTraderInfoFetched(PublicTraderInfo info, GetPublicTraderInfo request) {
      onLtGenericSuccess(request);
   }

   public void onLtTradeReceivingAddressSet(SetTradeReceivingAddress request) {
      onLtGenericSuccess(request);
   }

   public void onNotificationEmailSet(SetNotificationMail request) {
      onLtGenericSuccess(request);
   }
}
