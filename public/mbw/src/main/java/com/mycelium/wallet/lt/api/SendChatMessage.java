package com.mycelium.wallet.lt.api;

import java.util.Collection;
import java.util.UUID;

import com.mycelium.lt.api.LtApi;
import com.mycelium.lt.api.LtApiException;
import com.mycelium.lt.api.params.ChatMessageParameters;
import com.mycelium.wallet.lt.LocalTraderEventSubscriber;
import com.mycelium.wallet.lt.LocalTraderManager.LocalManagerApiContext;

public class SendChatMessage extends Request {
   private static final long serialVersionUID = 1L;

   private UUID _tradeSessionId;
   private String _message;

   public SendChatMessage(UUID tradeSessionId, String message) {
      super(true, true);
      _tradeSessionId = tradeSessionId;
      _message = message;
   }

   @Override
   public void execute(LocalManagerApiContext context, LtApi api, UUID sessionId,
         Collection<LocalTraderEventSubscriber> subscribers) {

      try {
         // Call function
         ChatMessageParameters params = new ChatMessageParameters(_tradeSessionId, _message);
         api.sendChatMessage(sessionId, params).getResult();

         // Notify
         synchronized (subscribers) {
            for (final LocalTraderEventSubscriber s : subscribers) {
               s.getHandler().post(new Runnable() {

                  @Override
                  public void run() {
                     s.onLtChatMessageSent(SendChatMessage.this);
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