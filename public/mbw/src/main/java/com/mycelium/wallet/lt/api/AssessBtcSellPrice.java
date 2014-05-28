package com.mycelium.wallet.lt.api;

import java.util.Collection;
import java.util.UUID;

import com.mycelium.lt.api.LtApi;
import com.mycelium.lt.api.LtApiException;
import com.mycelium.lt.api.model.BtcSellPrice;
import com.mycelium.lt.api.params.BtcSellPriceParameters;
import com.mycelium.wallet.lt.LocalTraderEventSubscriber;
import com.mycelium.wallet.lt.LocalTraderManager.LocalManagerApiContext;

public class AssessBtcSellPrice extends Request {
   private static final long serialVersionUID = 1L;

   public final BtcSellPriceParameters params;

   public AssessBtcSellPrice(BtcSellPriceParameters params) {
      super(true, false);
      this.params = params;
   }

   @Override
   public void execute(LocalManagerApiContext context, LtApi api, UUID sessionId,
         Collection<LocalTraderEventSubscriber> subscribers) {

      try {

         // Call function
         final BtcSellPrice btcSellPrice = api.assessBtcSellPrice(sessionId, params).getResult();

         // Notify
         synchronized (subscribers) {
            for (final LocalTraderEventSubscriber s : subscribers) {
               s.getHandler().post(new Runnable() {

                  @Override
                  public void run() {
                     s.onLtBtcSellPriceAssesed(btcSellPrice, AssessBtcSellPrice.this);
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