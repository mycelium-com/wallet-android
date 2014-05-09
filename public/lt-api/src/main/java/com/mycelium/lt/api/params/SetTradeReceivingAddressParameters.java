package com.mycelium.lt.api.params;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mrd.bitlib.model.Address;

public class SetTradeReceivingAddressParameters {
   @JsonProperty
   public UUID tradeSessionId;
   @JsonProperty
   public Address address;

   public SetTradeReceivingAddressParameters(@JsonProperty("tradeSessionId") UUID tradeSessionId,
         @JsonProperty("address") Address address) {
      this.tradeSessionId = tradeSessionId;
      this.address = address;
   }

   @SuppressWarnings("unused")
   private SetTradeReceivingAddressParameters() {
      // For Jackson
   }
}
