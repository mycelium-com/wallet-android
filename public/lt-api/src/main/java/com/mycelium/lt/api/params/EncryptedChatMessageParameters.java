package com.mycelium.lt.api.params;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mycelium.lt.ChatMessageEncryptionKey;

public class EncryptedChatMessageParameters {
   @JsonProperty
   public UUID tradeSessionId;
   @JsonProperty
   public String encryptedMessage;

   private EncryptedChatMessageParameters(@JsonProperty("tradeSessionId") UUID tradeSessionId,
         @JsonProperty("encryptedMessage") String encryptedMessage) {
      this.tradeSessionId = tradeSessionId;
      this.encryptedMessage = encryptedMessage;
   }

   public static EncryptedChatMessageParameters fromPlaintextParameters(UUID tradeSessionId, String message,
         ChatMessageEncryptionKey key) {
      String encryptedMessage = key.encryptChatMessage(message);
      return new EncryptedChatMessageParameters(tradeSessionId, encryptedMessage);
   }

   private EncryptedChatMessageParameters() {
      // For Jackson
   }
}
