package com.mycelium.wapi.api.response;

import java.io.Serializable;
import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mycelium.wapi.model.TransactionStatus;

public class CheckTransactionsResponse implements Serializable {
   private static final long serialVersionUID = 1L;

   @JsonProperty
   public final Collection<TransactionStatus> transactions;

   public CheckTransactionsResponse(@JsonProperty("transactions") Collection<TransactionStatus> transactions) {
      this.transactions = transactions;
   }

}
