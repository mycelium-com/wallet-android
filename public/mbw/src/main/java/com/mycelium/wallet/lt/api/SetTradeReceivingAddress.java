package com.mycelium.wallet.lt.api;

import java.util.Collection;
import java.util.UUID;

import com.mrd.bitlib.model.Address;
import com.mycelium.lt.api.LtApi;
import com.mycelium.lt.api.LtApiException;
import com.mycelium.lt.api.params.SetTradeReceivingAddressParameters;
import com.mycelium.wallet.lt.LocalTraderEventSubscriber;
import com.mycelium.wallet.lt.LocalTraderManager.LocalManagerApiContext;

public class SetTradeReceivingAddress extends Request {
   private static final long serialVersionUID = 1L;

   public UUID tradeSessionId;
   public Address address;

   public SetTradeReceivingAddress(UUID tradeSessionId, Address address) {
      super(true, true);
      this.tradeSessionId = tradeSessionId;
      this.address = address;
   }

   @Override
   public void execute(LocalManagerApiContext context, LtApi api, UUID sessionId,
         Collection<LocalTraderEventSubscriber> subscribers) {

      try {

         // Call function
         SetTradeReceivingAddressParameters params = new SetTradeReceivingAddressParameters(tradeSessionId, address);
         api.setTradeReceivingAddress(sessionId, params).getResult();

         // Notify
         synchronized (subscribers) {
            for (final LocalTraderEventSubscriber s : subscribers) {
               s.getHandler().post(new Runnable() {

                  @Override
                  public void run() {
                     s.onLtTradeReceivingAddressSet(SetTradeReceivingAddress.this);
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