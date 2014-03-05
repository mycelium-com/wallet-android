package com.mycelium.lt.api.params;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mrd.bitlib.model.Address;

public class InstantBuyOrderParameters {
   @JsonProperty
   public UUID sellOrderId;
   @JsonProperty
   public int fiatOffered;
   @JsonProperty
   public Address address;

   public InstantBuyOrderParameters(@JsonProperty("sellOrderId") UUID sellOrderId,
         @JsonProperty("fiatOffered") int fiatOffered, @JsonProperty("address") Address address) {
      this.sellOrderId = sellOrderId;
      this.address = address;
      this.fiatOffered = fiatOffered;
   }

   @SuppressWarnings("unused")
   private InstantBuyOrderParameters() {
      // For Jackson
   }
}
