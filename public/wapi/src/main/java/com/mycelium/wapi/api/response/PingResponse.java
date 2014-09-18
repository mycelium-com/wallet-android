package com.mycelium.wapi.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mycelium.wapi.model.TransactionEx;

import java.io.Serializable;
import java.util.Collection;

public class PingResponse implements Serializable {
   private static final long serialVersionUID = 1L;

   @JsonProperty
   public final String state;

   public PingResponse(@JsonProperty("state") String state) {
      this.state = state;
   }

}
