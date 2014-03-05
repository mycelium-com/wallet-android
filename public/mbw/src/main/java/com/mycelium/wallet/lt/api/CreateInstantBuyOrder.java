package com.mycelium.wallet.lt.api;

import java.util.Collection;
import java.util.UUID;

import com.mrd.bitlib.model.Address;
import com.mycelium.lt.api.LtApi;
import com.mycelium.lt.api.LtApiException;
import com.mycelium.lt.api.params.InstantBuyOrderParameters;
import com.mycelium.wallet.lt.LocalTraderEventSubscriber;
import com.mycelium.wallet.lt.LocalTraderManager.LocalManagerApiContext;

public class CreateInstantBuyOrder extends Request {
   private static final long serialVersionUID = 1L;

   private UUID _sellOrderId;
   private int _fiatOffered;
   private Address _address;

   public CreateInstantBuyOrder(UUID sellOrderId, int fiatOffered, Address address) {
      super(true, true);
      _sellOrderId = sellOrderId;
      _fiatOffered = fiatOffered;
      _address = address;
   }

   @Override
   public void execute(LocalManagerApiContext context, LtApi api, UUID sessionId,
         Collection<LocalTraderEventSubscriber> subscribers) {

      try {

         // Call function
         InstantBuyOrderParameters params = new InstantBuyOrderParameters(_sellOrderId, _fiatOffered, _address);
         final UUID tradeSessionId = api.createInstantBuyOrder(sessionId, params).getResult();

         // Notify
         synchronized (subscribers) {
            for (final LocalTraderEventSubscriber s : subscribers) {
               s.getHandler().post(new Runnable() {

                  @Override
                  public void run() {
                     s.onLtInstantBuyOrderCreated(tradeSessionId, CreateInstantBuyOrder.this);
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