package com.mycelium.wapi.api.request;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mrd.bitlib.util.Sha256Hash;

public class CheckTransactionsRequest implements Serializable {
   private static final long serialVersionUID = 1L;

   @JsonProperty
   public final List<Sha256Hash> txIds;

   public CheckTransactionsRequest(@JsonProperty("txIds") List<Sha256Hash> txIds) {
      this.txIds = txIds;
   }

}
