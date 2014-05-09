package com.mycelium.wallet.lt.api;

import java.util.Collection;
import java.util.UUID;

import com.mycelium.lt.api.LtApi;
import com.mycelium.lt.api.LtApiException;
import com.mycelium.lt.api.model.AdType;
import com.mycelium.lt.api.model.GpsLocation;
import com.mycelium.lt.api.params.AdParameters;
import com.mycelium.wallet.lt.LocalTraderEventSubscriber;
import com.mycelium.wallet.lt.LocalTraderManager.LocalManagerApiContext;

public class EditAd extends Request {
   private static final long serialVersionUID = 1L;

   public final UUID adId;
   public final AdType type;
   public final GpsLocation location;
   public final String currency;
   public final int minimumFiat;
   public final int maximumFiat;
   public final String priceFormulaId;
   public final double premium;
   public final String description;

   public EditAd(UUID adId, AdType type, GpsLocation location, String currency, int minimumFiat, int maximumFiat,
         String priceFormulaId, double premium, String description) {
      super(true, true);
      this.adId = adId;
      this.type = type;
      this.location = location;
      this.currency = currency;
      this.minimumFiat = minimumFiat;
      this.maximumFiat = maximumFiat;
      this.priceFormulaId = priceFormulaId;
      this.premium = premium;
      this.description = description;
   }

   @Override
   public void execute(LocalManagerApiContext context, LtApi api, UUID sessionId,
         Collection<LocalTraderEventSubscriber> subscribers) {

      try {

         // Call function
         AdParameters params = new AdParameters(type, location, currency, minimumFiat, maximumFiat, priceFormulaId,
               premium, description);
         api.editAd(sessionId, adId, params).getResult();

         // Notify
         synchronized (subscribers) {
            for (final LocalTraderEventSubscriber s : subscribers) {
               s.getHandler().post(new Runnable() {

                  @Override
                  public void run() {
                     s.onLtAdEdited(EditAd.this);
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