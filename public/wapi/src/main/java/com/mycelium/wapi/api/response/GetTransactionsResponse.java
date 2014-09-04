package com.mycelium.wapi.api.response;

import java.io.Serializable;
import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mycelium.wapi.model.TransactionEx;

public class GetTransactionsResponse implements Serializable {
   private static final long serialVersionUID = 1L;

   @JsonProperty
   public final Collection<TransactionEx> transactions;

   public GetTransactionsResponse(@JsonProperty("transactions") Collection<TransactionEx> transactions) {
      this.transactions = transactions;
   }

}
