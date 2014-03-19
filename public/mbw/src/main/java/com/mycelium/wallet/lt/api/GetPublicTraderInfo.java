package com.mycelium.wallet.lt.api;

import java.util.Collection;
import java.util.UUID;

import com.google.common.base.Preconditions;
import com.mrd.bitlib.model.Address;
import com.mycelium.lt.api.LtApi;
import com.mycelium.lt.api.LtApiException;
import com.mycelium.lt.api.model.PublicTraderInfo;
import com.mycelium.wallet.lt.LocalTraderEventSubscriber;
import com.mycelium.wallet.lt.LocalTraderManager.LocalManagerApiContext;

public class GetPublicTraderInfo extends Request {
   private static final long serialVersionUID = 1L;

   private Address _traderIdentity;

   public GetPublicTraderInfo(Address traderIdentity) {
      super(true, true);
      _traderIdentity = Preconditions.checkNotNull(traderIdentity);
   }

   @Override
   public void execute(LocalManagerApiContext context, LtApi api, UUID sessionId,
         Collection<LocalTraderEventSubscriber> subscribers) {

      try {

         notifySendingRequest(subscribers);

         // Call function
         final PublicTraderInfo info = api.getPublicTraderInfo(sessionId, _traderIdentity).getResult();

         // Notify Success
         synchronized (subscribers) {
            for (final LocalTraderEventSubscriber s : subscribers) {
               s.getHandler().post(new Runnable() {

                  @Override
                  public void run() {
                     s.onLtPublicTraderInfoFetched(info, GetPublicTraderInfo.this);
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