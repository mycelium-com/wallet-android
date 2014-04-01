package com.mycelium.wallet.lt.api;

import java.util.Collection;
import java.util.UUID;

import com.mycelium.lt.ChatMessageEncryptionKey;
import com.mycelium.lt.api.LtApi;
import com.mycelium.lt.api.LtApiException;
import com.mycelium.lt.api.params.EncryptedChatMessageParameters;
import com.mycelium.wallet.lt.LocalTraderEventSubscriber;
import com.mycelium.wallet.lt.LocalTraderManager.LocalManagerApiContext;

public class SendEncryptedChatMessage extends Request {
   private static final long serialVersionUID = 1L;

   private UUID _tradeSessionId;
   private String _message;
   private ChatMessageEncryptionKey _key;

   public SendEncryptedChatMessage(UUID tradeSessionId, String message, ChatMessageEncryptionKey key) {
      super(true, true);
      _tradeSessionId = tradeSessionId;
      _message = message;
      _key = key;
   }

   @Override
   public void execute(LocalManagerApiContext context, LtApi api, UUID sessionId,
         Collection<LocalTraderEventSubscriber> subscribers) {

      try {
         // Call function
         EncryptedChatMessageParameters params = EncryptedChatMessageParameters.fromPlaintextParameters(
               _tradeSessionId, _message, _key);
         api.sendEncryptedChatMessage(sessionId, params).getResult();

         // Notify
         synchronized (subscribers) {
            for (final LocalTraderEventSubscriber s : subscribers) {
               s.getHandler().post(new Runnable() {

                  @Override
                  public void run() {
                     s.onLtEncryptedChatMessageSent(SendEncryptedChatMessage.this);
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