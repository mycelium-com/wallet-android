package com.mycelium.wapi.api.request;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mrd.bitlib.model.Address;

public class QueryTransactionInventoryRequest implements Serializable {
   private static final long serialVersionUID = 1L;

   @JsonProperty
   public final int version;
   @JsonProperty
   public final List<Address> addresses;

   public QueryTransactionInventoryRequest(@JsonProperty("version") int version,
         @JsonProperty("addresses") List<Address> addresses) {
      this.version = version;
      this.addresses = addresses;
   }

}
