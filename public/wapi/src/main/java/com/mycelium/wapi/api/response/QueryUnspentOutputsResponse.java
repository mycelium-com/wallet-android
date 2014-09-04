package com.mycelium.wapi.api.response;

import java.io.Serializable;
import java.util.Collection;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mycelium.wapi.model.TransactionOutputEx;

public class QueryUnspentOutputsResponse implements Serializable {
   private static final long serialVersionUID = 1L;

   @JsonProperty
   public final int height;
   @JsonProperty
   public final Collection<TransactionOutputEx> unspent;

   public QueryUnspentOutputsResponse(@JsonProperty("height") int height,
         @JsonProperty("unspent") Collection<TransactionOutputEx> unspent) {
      this.height = height;
      this.unspent = unspent;
   }

}
