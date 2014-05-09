package com.mycelium.lt.api.params;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateTradeParameters {
   @JsonProperty
   public UUID adId;
   @JsonProperty
   public int fiatOffered;

   public CreateTradeParameters(@JsonProperty("adId") UUID adId, @JsonProperty("fiatOffered") int fiatOffered) {
      this.adId = adId;
      this.fiatOffered = fiatOffered;
   }

   @SuppressWarnings("unused")
   private CreateTradeParameters() {
      // For Jackson
   }
}
