package com.mycelium.lt.api.params;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ChatMessageParameters {
   @JsonProperty
   public UUID tradeSessionId;
   @JsonProperty
   public String message;

   public ChatMessageParameters(@JsonProperty("tradeSessionId") UUID tradeSessionId,
         @JsonProperty("message") String message) {
      this.tradeSessionId = tradeSessionId;
      this.message = message;
   }

   @SuppressWarnings("unused")
   private ChatMessageParameters() {
      // For Jackson
   }
}
