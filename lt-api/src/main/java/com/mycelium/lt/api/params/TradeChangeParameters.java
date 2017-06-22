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

import java.io.Serializable;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TradeChangeParameters implements Serializable {
   private static final long serialVersionUID = 1L;

   @JsonProperty
   public UUID tradeSessionId;
   @JsonProperty
   public String priceFormulaId;
   @JsonProperty
   public double premium;

   public TradeChangeParameters(@JsonProperty("tradeSessionId") UUID tradeSessionId,
         @JsonProperty("priceFormulaId") String priceFormulaId, @JsonProperty("premium") double premium) {
      this.tradeSessionId = tradeSessionId;
      this.priceFormulaId = priceFormulaId;
      this.premium = premium;
   }

   @SuppressWarnings("unused")
   private TradeChangeParameters() {
      // For Jackson
   }
}
