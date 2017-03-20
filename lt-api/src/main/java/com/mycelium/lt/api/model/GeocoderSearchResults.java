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

package com.mycelium.lt.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mycelium.lt.location.GeocodeResponse;

import java.io.Serializable;

public class GeocoderSearchResults implements Serializable {
   private static final long serialVersionUID = 1L;

   @JsonProperty
   public GeocodeResponse results;

   public GeocoderSearchResults() {
   }

   public GeocoderSearchResults(GeocodeResponse results) {
      this.results = results;
   }

}
