package com.mycelium.lt.api.params;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mycelium.lt.api.model.AdType;
import com.mycelium.lt.api.model.GpsLocation;

public class SearchParameters {
   @JsonProperty
   public GpsLocation location;
   @JsonProperty
   public int limit;
   @JsonProperty
   public AdType type;

   public SearchParameters(@JsonProperty("location") GpsLocation location, @JsonProperty("limit") int limit, @JsonProperty("type") AdType type) {
      this.location = location;
      this.limit = limit;
      this.type = type;
   }

   @SuppressWarnings("unused")
   private SearchParameters() {
      // For Jackson
   }

}
