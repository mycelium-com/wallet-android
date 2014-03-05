package com.mycelium.wallet.lt.api;

import java.util.Collection;
import java.util.UUID;

import com.mycelium.lt.api.LtApi;
import com.mycelium.lt.api.LtApiException;
import com.mycelium.wallet.lt.LocalTraderEventSubscriber;
import com.mycelium.wallet.lt.LocalTraderManager.LocalManagerApiContext;

public class AcceptTrade extends Request {
   private static final long serialVersionUID = 1L;

   private UUID _tradeSessionId;
   private long _tradeSessionTimestamp;

   public AcceptTrade(UUID tradeSessionId, long tradeSessionTimestamp) {
      super(true, true);
      _tradeSessionId = tradeSessionId;
      _tradeSessionTimestamp = tradeSessionTimestamp;
   }

   @Override
   public void execute(LocalManagerApiContext context, LtApi api, UUID sessionId,
         Collection<LocalTraderEventSubscriber> subscribers) {

      try {
         // Call function
         api.acceptTrade(sessionId, _tradeSessionId, _tradeSessionTimestamp).getResult();

         // Notify
         synchronized (subscribers) {
            for (final LocalTraderEventSubscriber s : subscribers) {
               s.getHandler().post(new Runnable() {

                  @Override
                  public void run() {
                     s.onLtTradeSessionAccepted(_tradeSessionId, AcceptTrade.this);
                  }
               });
            }
         }

      } catch (LtApiException e) {
         // Handle errors
         context.handleErrors(this, e.errorCode);
      }

   }
}