package com.mycelium.wallet.lt.api;

import java.util.Collection;
import java.util.UUID;

import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;
import com.mycelium.lt.ApiUtils;
import com.mycelium.lt.api.LtApi;
import com.mycelium.lt.api.LtApiException;
import com.mycelium.lt.api.params.TraderParameters;
import com.mycelium.wallet.AndroidRandomSource;
import com.mycelium.wallet.lt.LocalTraderEventSubscriber;
import com.mycelium.wallet.lt.LocalTraderManager.LocalManagerApiContext;

public class CreateTrader extends Request {
   private static final long serialVersionUID = 1L;

   public InMemoryPrivateKey _privateKey;
   public String _nickname;
   public String _locale;
   private NetworkParameters _network;

   public CreateTrader(InMemoryPrivateKey privateKey, String nickname, String locale, NetworkParameters network) {
      super(true, false);
      _privateKey = privateKey;
      _nickname = nickname;
      _locale = locale;
      _network = network;
   }

   @Override
   public void execute(LocalManagerApiContext context, LtApi api, UUID sessionId,
         Collection<LocalTraderEventSubscriber> subscribers) {

      // Sign session ID with private key
      String sigHashSessionId = ApiUtils.generateUuidHashSignature(_privateKey, sessionId, new AndroidRandomSource());
      Address address = _privateKey.getPublicKey().toAddress(_network);

      try {

         // Call function
         TraderParameters params = new TraderParameters(_nickname, address, sigHashSessionId);
         api.createTrader(sessionId, params).getResult();

         // Notify Success
         synchronized (subscribers) {
            for (final LocalTraderEventSubscriber s : subscribers) {
               s.getHandler().post(new Runnable() {

                  @Override
                  public void run() {
                     s.onLtTraderCreated(CreateTrader.this);
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