package com.mycelium.wallet.lt.api;

import java.io.Serializable;
import java.util.Collection;
import java.util.UUID;

import com.mycelium.lt.api.LtApi;
import com.mycelium.wallet.lt.LocalTraderEventSubscriber;
import com.mycelium.wallet.lt.LocalTraderManager.LocalManagerApiContext;

public abstract class Request implements Serializable{
   private static final long serialVersionUID = 1L;
   
   private boolean _requiresSession;
   private boolean _requiresLogin;

   protected Request(boolean requiresSession, boolean requiresLogin) {
      _requiresSession = requiresSession;
      _requiresLogin = requiresLogin;
   }

   public boolean requiresSession() {
      return _requiresSession;
   }

   public boolean requiresLogin() {
      return _requiresLogin;
   }

   public abstract void execute(LocalManagerApiContext context, LtApi api, UUID sessionId,
         Collection<LocalTraderEventSubscriber> subscribers);

   protected void notifySendingRequest(Collection<LocalTraderEventSubscriber> subscribers){
      // Notify Success
      synchronized (subscribers) {
         for (final LocalTraderEventSubscriber s : subscribers) {
            s.getHandler().post(new Runnable() {

               @Override
               public void run() {
                  s.onLtSendingRequest(Request.this);
               }
            });
         }
      }
      
   }
}