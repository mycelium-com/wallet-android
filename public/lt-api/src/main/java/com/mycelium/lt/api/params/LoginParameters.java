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
import com.mrd.bitlib.model.Address;

public class LoginParameters {
   @JsonProperty
   public Address address;
   @JsonProperty
   public String signature;
   @JsonProperty
   private String gcmId;
   @JsonProperty
   private long lastTradeSessionChange;

   public LoginParameters(@JsonProperty("address") Address address, @JsonProperty("signature") String signature) {
      this.address = address;
      this.signature = signature;
   }

   public String getGcmId() {
      return gcmId;
   }

   public void setGcmId(String gcmId) {
      this.gcmId = gcmId;
   }

   public long getLastTradeSessionChange() {
      return lastTradeSessionChange;
   }

   public void setLastTradeSessionChange(long lastTradeSessionChange) {
      this.lastTradeSessionChange = lastTradeSessionChange;
   }

   @SuppressWarnings("unused")
   private LoginParameters() {
      // For Jackson
   }

}
