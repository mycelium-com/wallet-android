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

import java.io.Serializable;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Ad implements Serializable {
   private static final long serialVersionUID = 1L;

   @JsonProperty
   public final UUID id;
   @JsonProperty
   public final AdType type;
   @JsonProperty
   public GpsLocation location;
   @JsonProperty
   public String currency;
   @JsonProperty
   public int minimumFiat;
   @JsonProperty
   public int maximumFiat;
   @JsonProperty
   public PriceFormula priceFormula;
   @JsonProperty
   public double premium;
   @JsonProperty
   public String description;
   @JsonProperty
   public boolean isActive;
   @JsonProperty
   public long deactivationTime;

   public Ad(@JsonProperty("id") UUID id, @JsonProperty("type") AdType type,
         @JsonProperty("location") GpsLocation location, @JsonProperty("currency") String currency,
         @JsonProperty("minimumFiat") int minimumFiat, @JsonProperty("maximumFiat") int maximumFiat,
         @JsonProperty("priceFormula") PriceFormula priceFormula, @JsonProperty("premium") double premium,
         @JsonProperty("description") String description, @JsonProperty("isActive") boolean isActive,
         @JsonProperty("deactivationTime") long deactivationTime) {
      this.id = id;
      this.type = type;
      this.location = location;
      this.currency = currency;
      this.minimumFiat = minimumFiat;
      this.maximumFiat = maximumFiat;
      this.priceFormula = priceFormula;
      this.premium = premium;
      this.description = description;
      this.isActive = isActive;
      this.deactivationTime = deactivationTime;
   }

}
