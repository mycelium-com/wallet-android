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

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mrd.bitlib.model.Address;

public class SetTradeReceivingAddressParameters {
   @JsonProperty
   public UUID tradeSessionId;
   @JsonProperty
   public Address address;

   public SetTradeReceivingAddressParameters(@JsonProperty("tradeSessionId") UUID tradeSessionId,
         @JsonProperty("address") Address address) {
      this.tradeSessionId = tradeSessionId;
      this.address = address;
   }

   @SuppressWarnings("unused")
   private SetTradeReceivingAddressParameters() {
      // For Jackson
   }
}
