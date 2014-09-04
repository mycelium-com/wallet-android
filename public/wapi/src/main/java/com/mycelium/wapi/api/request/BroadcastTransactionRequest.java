package com.mycelium.wapi.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BroadcastTransactionRequest {
   @JsonProperty
   public final int version;
   @JsonProperty
   public final byte[] rawTransaction;

   public BroadcastTransactionRequest(@JsonProperty("version") int version,
         @JsonProperty("rawTransaction") byte[] rawTransaction) {
      this.version = version;
      this.rawTransaction = rawTransaction;
   }

}
