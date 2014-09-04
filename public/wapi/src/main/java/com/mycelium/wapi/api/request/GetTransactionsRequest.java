package com.mycelium.wapi.api.request;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mrd.bitlib.util.Sha256Hash;

public class GetTransactionsRequest {
   @JsonProperty
   public final int version;
   @JsonProperty
   public final Collection<Sha256Hash> txIds;

   public GetTransactionsRequest(@JsonProperty("version") int version,
         @JsonProperty("txIds") Collection<Sha256Hash> txIds) {
      this.version = version;
      this.txIds = txIds;
   }

}
