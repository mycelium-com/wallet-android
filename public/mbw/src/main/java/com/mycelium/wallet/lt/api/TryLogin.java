package com.mycelium.wallet.lt.api;

import java.util.Collection;
import java.util.UUID;

import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;
import com.mycelium.lt.ApiUtils;
import com.mycelium.lt.api.LtApi;
import com.mycelium.lt.api.LtApiException;
import com.mycelium.lt.api.params.LoginParameters;
import com.mycelium.wallet.AndroidRandomSource;
import com.mycelium.wallet.lt.LocalTraderEventSubscriber;
import com.mycelium.wallet.lt.LocalTraderManager.LocalManagerApiContext;

public class TryLogin extends Request {
   private static final long serialVersionUID = 1L;

   private InMemoryPrivateKey _privateKey;
   private NetworkParameters _network;

   public TryLogin(InMemoryPrivateKey privateKey, NetworkParameters network) {
      super(true, false);
      _privateKey = privateKey;
      _network = network;
   }

   @Override
   public void execute(LocalManagerApiContext context, LtApi api, UUID sessionId,
         Collection<LocalTraderEventSubscriber> subscribers) {

      // Sign session ID with private key
      String signedMessage = ApiUtils.generateUuidHashSignature(_privateKey, sessionId, new AndroidRandomSource());
      try {

         // Call function
         Address address = _privateKey.getPublicKey().toAddress(_network);
         LoginParameters params = new LoginParameters(address, signedMessage);
         final String nickname = api.traderLogin(sessionId, params).getResult();

         // Notify Login success
         synchronized (subscribers) {
            for (final LocalTraderEventSubscriber s : subscribers) {
               s.getHandler().post(new Runnable() {

                  @Override
                  public void run() {
                     s.onLtLogin(nickname, TryLogin.this);
                  }
               });
            }
         }

      } catch (LtApiException e) {
         // Handle errors
         if (e.errorCode == LtApi.ERROR_CODE_TRADER_DOES_NOT_EXIST) {
            // Notify Login failure
            synchronized (subscribers) {
               for (final LocalTraderEventSubscriber s : subscribers) {
                  s.getHandler().post(new Runnable() {

                     @Override
                     public void run() {
                        s.onLtNoTraderAccount();
                     }
                  });
               }
            }
         } else {
            // Handle other errors
            context.handleErrors(this, e.errorCode);
         }
      }

   }
}