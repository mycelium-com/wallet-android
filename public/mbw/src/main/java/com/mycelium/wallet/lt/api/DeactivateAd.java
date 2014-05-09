package com.mycelium.wallet.lt.api;

import java.util.Collection;
import java.util.UUID;

import com.mycelium.lt.api.LtApi;
import com.mycelium.lt.api.LtApiException;
import com.mycelium.wallet.lt.LocalTraderEventSubscriber;
import com.mycelium.wallet.lt.LocalTraderManager.LocalManagerApiContext;

public class DeactivateAd extends Request {
   private static final long serialVersionUID = 1L;

   private UUID _adId;

   public DeactivateAd(UUID adId) {
      super(true, true);
      _adId = adId;
   }

   @Override
   public void execute(LocalManagerApiContext context, LtApi api, UUID sessionId,
         Collection<LocalTraderEventSubscriber> subscribers) {

      try {
         // Call function
         api.deactivateAd(sessionId, _adId).getResult();

         // Notify
         synchronized (subscribers) {
            for (final LocalTraderEventSubscriber s : subscribers) {
               s.getHandler().post(new Runnable() {

                  @Override
                  public void run() {
                     s.onLtAdDeactivated(_adId, DeactivateAd.this);
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