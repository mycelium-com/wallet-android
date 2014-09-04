package com.mycelium.wapi.api.response;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mrd.bitlib.util.Sha256Hash;

public class QueryTransactionInventoryResponse implements Serializable {
   private static final long serialVersionUID = 1L;

   @JsonProperty
   public final int height;
   @JsonProperty
   public final List<Sha256Hash> txIds;

   public QueryTransactionInventoryResponse(@JsonProperty("height") int height,
         @JsonProperty("txIds") List<Sha256Hash> txIds) {
      this.height = height;
      this.txIds = txIds;
   }

}
