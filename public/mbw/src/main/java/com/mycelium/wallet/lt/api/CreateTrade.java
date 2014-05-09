package com.mycelium.wallet.lt.api;

import java.util.Collection;
import java.util.UUID;

import com.mycelium.lt.api.LtApi;
import com.mycelium.lt.api.LtApiException;
import com.mycelium.lt.api.params.CreateTradeParameters;
import com.mycelium.wallet.lt.LocalTraderEventSubscriber;
import com.mycelium.wallet.lt.LocalTraderManager.LocalManagerApiContext;

public class CreateTrade extends Request {
   private static final long serialVersionUID = 1L;

   private UUID _adId;
   private int _fiatOffered;

   public CreateTrade(UUID adId, int fiatOffered) {
      super(true, true);
      _adId = adId;
      _fiatOffered = fiatOffered;
   }

   @Override
   public void execute(LocalManagerApiContext context, LtApi api, UUID sessionId,
         Collection<LocalTraderEventSubscriber> subscribers) {

      try {

         // Call function
         CreateTradeParameters params = new CreateTradeParameters(_adId, _fiatOffered);
         final UUID tradeSessionId = api.createTrade(sessionId, params).getResult();

         // Notify
         synchronized (subscribers) {
            for (final LocalTraderEventSubscriber s : subscribers) {
               s.getHandler().post(new Runnable() {

                  @Override
                  public void run() {
                     s.onLtTradeCreated(tradeSessionId, CreateTrade.this);
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