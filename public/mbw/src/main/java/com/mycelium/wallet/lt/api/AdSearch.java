package com.mycelium.wallet.lt.api;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.mycelium.lt.api.LtApi;
import com.mycelium.lt.api.LtApiException;
import com.mycelium.lt.api.model.AdSearchItem;
import com.mycelium.lt.api.model.AdType;
import com.mycelium.lt.api.model.GpsLocation;
import com.mycelium.lt.api.params.SearchParameters;
import com.mycelium.wallet.lt.LocalTraderEventSubscriber;
import com.mycelium.wallet.lt.LocalTraderManager.LocalManagerApiContext;

public class AdSearch extends Request {
   private static final long serialVersionUID = 1L;

   public final GpsLocation location;
   public final int limit;
   public AdType type;

   public AdSearch(GpsLocation location, int limit, AdType type) {
      super(true, false);
      this.location = location;
      this.limit = limit;
      this.type = type;
   }

   @Override
   public void execute(LocalManagerApiContext context, LtApi api, UUID sessionId,
         Collection<LocalTraderEventSubscriber> subscribers) {

      try {

         // Call function
         SearchParameters params = new SearchParameters(location, limit, type);
         final List<AdSearchItem> list = api.adSearch(sessionId, params).getResult();

         // Notify Success
         synchronized (subscribers) {
            for (final LocalTraderEventSubscriber s : subscribers) {
               s.getHandler().post(new Runnable() {

                  @Override
                  public void run() {
                     s.onLtAdSearch(list, AdSearch.this);
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