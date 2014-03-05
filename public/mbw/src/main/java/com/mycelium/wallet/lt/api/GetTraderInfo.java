package com.mycelium.wallet.lt.api;

import java.util.Collection;
import java.util.UUID;

import com.mycelium.lt.api.LtApi;
import com.mycelium.lt.api.LtApiException;
import com.mycelium.lt.api.model.TraderInfo;
import com.mycelium.wallet.lt.LocalTraderEventSubscriber;
import com.mycelium.wallet.lt.LocalTraderManager.LocalManagerApiContext;

public class GetTraderInfo extends Request {
   private static final long serialVersionUID = 1L;

   public GetTraderInfo() {
      super(true, true);
   }

   @Override
   public void execute(LocalManagerApiContext context, LtApi api, UUID sessionId,
         Collection<LocalTraderEventSubscriber> subscribers) {

      try {

         notifySendingRequest(subscribers);

         // Call function
         final TraderInfo info = api.getTraderInfo(sessionId).getResult();

         // Cache TraderInfo
         context.cacheTraderInfo(info);

         // Update database
         context.updateLocalTradeSessions(info.activeTradeSesions);

         // Notify Success
         synchronized (subscribers) {
            for (final LocalTraderEventSubscriber s : subscribers) {
               s.getHandler().post(new Runnable() {

                  @Override
                  public void run() {
                     s.onLtTraderInfoFetched(info, GetTraderInfo.this);
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