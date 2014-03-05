package com.mycelium.lt.api.params;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ReleaseBtcParameters {
   @JsonProperty
   public UUID tradeSessionId;
   @JsonProperty
   public String rawHexTransaction;

   public ReleaseBtcParameters(@JsonProperty("tradeSessionId") UUID tradeSessionId,
         @JsonProperty("rawHexTransaction") String rawHexTransaction) {
      this.tradeSessionId = tradeSessionId;
      this.rawHexTransaction = rawHexTransaction;
   }

   @SuppressWarnings("unused")
   private ReleaseBtcParameters() {
      // For Jackson
   }
}
