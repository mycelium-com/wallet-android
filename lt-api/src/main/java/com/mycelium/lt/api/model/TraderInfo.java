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
import java.util.LinkedList;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mrd.bitlib.crypto.PublicKey;
import com.mrd.bitlib.model.Address;

public class TraderInfo extends PublicTraderInfo implements Serializable {
   private static final long serialVersionUID = 1L;

   @JsonProperty
   public final double localTraderPremium;
   @JsonProperty
   public final long totalBtcBought;
   @JsonProperty
   public final long totalBtcSold;
   @JsonProperty
   public final LinkedList<TradeSession> activeTradeSesions;

   @JsonProperty
   public final String notificationEmail;

   public TraderInfo(@JsonProperty("nickname") String nickname, @JsonProperty("address") Address address,
         @JsonProperty("publicKey") PublicKey publicKey, @JsonProperty("traderAgeMs") long traderAgeMs,
         @JsonProperty("idleTime") long idleTime, @JsonProperty("lastChange") long lastChange,
         @JsonProperty("localTraderPremium") double localTraderPremium,
         @JsonProperty("successfulSales") int successfulSales, @JsonProperty("successfulBuys") int successfulBuys,
         @JsonProperty("abortedSales") int abortedSales, @JsonProperty("abortedBuys") int abortedBuys,
         @JsonProperty("totalBtcBought") long totalBtcBought, @JsonProperty("totalBtcSold") long totalBtcSold,
         @JsonProperty("tradeMedianMs") Long tradeMedianMs,
         @JsonProperty("activeTradeSesions") LinkedList<TradeSession> activeTradeSesions,
         @JsonProperty("notificatonEmail") String notificationEmail) {
      super(nickname, address, publicKey, traderAgeMs, idleTime, lastChange, successfulSales, successfulBuys,
            abortedSales, abortedBuys, tradeMedianMs);
      this.localTraderPremium = localTraderPremium;
      this.totalBtcBought = totalBtcBought;
      this.totalBtcSold = totalBtcSold;
      this.activeTradeSesions = activeTradeSesions;
      this.notificationEmail = notificationEmail;
   }
}
