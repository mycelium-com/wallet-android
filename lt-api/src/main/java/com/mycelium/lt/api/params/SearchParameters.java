/*
 * Copyright 2013, 2014 Megion Research & Development GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
