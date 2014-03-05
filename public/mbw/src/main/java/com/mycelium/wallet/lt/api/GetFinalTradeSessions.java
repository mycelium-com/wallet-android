package com.mycelium.wallet.lt.api;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.mycelium.lt.api.LtApi;
import com.mycelium.lt.api.LtApiException;
import com.mycelium.lt.api.model.TradeSessionStatus;
import com.mycelium.wallet.lt.LocalTraderEventSubscriber;
import com.mycelium.wallet.lt.LocalTraderManager.LocalManagerApiContext;

public class GetFinalTradeSessions extends Request {
   private static final long serialVersionUID = 1L;

   private int _limit;
   private int _offset;

   public GetFinalTradeSessions(int limit, int offset) {
      super(true, true);
      _limit = limit;
      _offset = offset;
   }

   @Override
   public void execute(LocalManagerApiContext context, LtApi api, UUID sessionId,
         Collection<LocalTraderEventSubscriber> subscribers) {

      try {

         // Call function
         final List<TradeSessionStatus> list = api.getFinalTradeSessions(sessionId, _limit, _offset).getResult();

         // Notify
         synchronized (subscribers) {
            for (final LocalTraderEventSubscriber s : subscribers) {
               s.getHandler().post(new Runnable() {

                  @Override
                  public void run() {
                     s.onLtFinalTradeSessionsFetched(list, GetFinalTradeSessions.this);
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