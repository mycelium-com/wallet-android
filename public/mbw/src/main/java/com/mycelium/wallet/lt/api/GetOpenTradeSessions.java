package com.mycelium.wallet.lt.api;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.mycelium.lt.api.LtApi;
import com.mycelium.lt.api.LtApiException;
import com.mycelium.lt.api.model.TradeSessionStatus;
import com.mycelium.wallet.lt.LocalTraderEventSubscriber;
import com.mycelium.wallet.lt.LocalTraderManager.LocalManagerApiContext;

public class GetOpenTradeSessions extends Request {
   private static final long serialVersionUID = 1L;

   public GetOpenTradeSessions() {
      super(true, true);
   }

   @Override
   public void execute(LocalManagerApiContext context, LtApi api, UUID sessionId,
         Collection<LocalTraderEventSubscriber> subscribers) {

      try {

         // Call function
         final List<TradeSessionStatus> list = api.getActiveTradeSessions(sessionId).getResult();

         // Update database
         context.updateLocalTradeSessions(list);

         // Notify
         synchronized (subscribers) {
            for (final LocalTraderEventSubscriber s : subscribers) {
               s.getHandler().post(new Runnable() {

                  @Override
                  public void run() {
                     s.onLtOpenTradeSessionsFetched(list, GetOpenTradeSessions.this);
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