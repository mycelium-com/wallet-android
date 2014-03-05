package com.mycelium.lt.api.params;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mycelium.lt.api.model.GpsLocation;

public class SearchParameters {
   @JsonProperty
   public GpsLocation location;
   @JsonProperty
   public int limit;

   public SearchParameters(@JsonProperty("location") GpsLocation location, @JsonProperty("limit") int limit) {
      this.location = location;
      this.limit = limit;
   }

   @SuppressWarnings("unused")
   private SearchParameters() {
      // For Jackson
   }

}
