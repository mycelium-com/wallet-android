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

import com.fasterxml.jackson.annotation.JsonProperty;

public class PriceFormula implements Serializable {
   private static final long serialVersionUID = 1L;

   @JsonProperty
   public final String id;
   @JsonProperty
   public final String name;
   @JsonProperty
   public final Boolean available;

   public PriceFormula(@JsonProperty("id") String id,
                       @JsonProperty("name") String name,
                       @JsonProperty("available") Boolean available) {
      this.id = id;
      this.name = name;
      this.available = available;
   }

   @Override
   public int hashCode() {
      return id.hashCode();
   }

   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof PriceFormula)) {
         return false;
      }
      return ((PriceFormula) obj).id.equals(id);
   }

}
