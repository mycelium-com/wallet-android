package com.mycelium.wapi.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mrd.bitlib.model.Address;

import java.io.Serializable;
import java.util.Collection;

public class QueryUnspentOutputsRequest implements Serializable {
   private static final long serialVersionUID = 1L;

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
