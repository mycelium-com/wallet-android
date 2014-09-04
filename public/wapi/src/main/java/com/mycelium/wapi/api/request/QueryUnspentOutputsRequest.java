package com.mycelium.wapi.api.request;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mrd.bitlib.model.Address;

public class QueryUnspentOutputsRequest {
   @JsonProperty
   public final int version;
   @JsonProperty
   public final Collection<Address> addresses;

   public QueryUnspentOutputsRequest(@JsonProperty("version") int version,
         @JsonProperty("address") Collection<Address> addresses) {
      this.version = version;
      this.addresses = addresses;
   }

}
