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
import com.mrd.bitlib.crypto.PublicKey;
import com.mrd.bitlib.model.Address;

public class PublicTraderInfo implements Serializable {
   private static final long serialVersionUID = 1L;

   @JsonProperty
   public String nickname;
   @JsonProperty
   public final Address address;
   @JsonProperty
   public final PublicKey publicKey;
   @JsonProperty
   public final long traderAgeMs;
   @JsonProperty
   public final long idleTime;
   @JsonProperty
   public final long lastChange;
   @JsonProperty
   public final int successfulSales;
   @JsonProperty
   public final int successfulBuys;
   @JsonProperty
   public final int abortedSales;
   @JsonProperty
   public final int abortedBuys;
   @JsonProperty
   public final Long tradeMedianMs;

   public PublicTraderInfo(@JsonProperty("nickname") String nickname, @JsonProperty("address") Address address, @JsonProperty("publicKey") PublicKey publicKey,
         @JsonProperty("traderAgeMs") long traderAgeMs, @JsonProperty("idleTime") long idleTime, @JsonProperty("lastChange") long lastChange,
         @JsonProperty("successfulSales") int successfulSales, @JsonProperty("successfulBuys") int successfulBuys,
         @JsonProperty("abortedSales") int abortedSales, @JsonProperty("abortedBuys") int abortedBuys,
         @JsonProperty("tradeMedianMs") Long tradeMedianMs) {
      this.nickname = nickname;
      this.address = address;
      this.publicKey = publicKey;
      this.traderAgeMs = traderAgeMs;
      this.idleTime = idleTime;
      this.lastChange = lastChange;
      this.successfulSales = successfulSales;
      this.successfulBuys = successfulBuys;
      this.abortedSales = abortedSales;
      this.abortedBuys = abortedBuys;
      this.tradeMedianMs = tradeMedianMs;
   }

}
