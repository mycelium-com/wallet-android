package com.mycelium.wallet.lt.api;

import java.util.Collection;
import java.util.UUID;

import com.mycelium.lt.api.LtApi;
import com.mycelium.lt.api.LtApiException;
import com.mycelium.lt.api.params.ReleaseBtcParameters;
import com.mycelium.wallet.lt.LocalTraderEventSubscriber;
import com.mycelium.wallet.lt.LocalTraderManager.LocalManagerApiContext;

public class ReleaseBtc extends Request {
   private static final long serialVersionUID = 1L;

   private UUID _tradeSessionId;
   private String _rawHexTransaction;

   public ReleaseBtc(UUID tradeSessionId, String rawHexTransaction) {
      super(true, true);
      _tradeSessionId = tradeSessionId;
      _rawHexTransaction = rawHexTransaction;
   }

   @Override
   public void execute(LocalManagerApiContext context, LtApi api, UUID sessionId,
         Collection<LocalTraderEventSubscriber> subscribers) {

      try {
         // Call function
         ReleaseBtcParameters params = new ReleaseBtcParameters(_tradeSessionId, _rawHexTransaction);
         final Boolean released = api.releaseBtc(sessionId, params).getResult();

         // XXX update local TX DB

         // Notify
         synchronized (subscribers) {
            for (final LocalTraderEventSubscriber s : subscribers) {
               s.getHandler().post(new Runnable() {

                  @Override
                  public void run() {
                     s.onLtBtcReleased(released, ReleaseBtc.this);
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