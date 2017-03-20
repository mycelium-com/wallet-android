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

package com.mycelium.wapi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;

import java.io.Serializable;
import java.util.Date;

public class ExchangeRate implements Serializable {
   private static final long serialVersionUID = 1L;

   @JsonProperty
   public final String name;
   @JsonProperty
   public final long time;
   @JsonProperty
   public final String currency;
   @JsonProperty
   public final Double price; // null if price is not available

   public ExchangeRate(@JsonProperty("name") String name, @JsonProperty("time") long time, @JsonProperty("price") double price, @JsonProperty("currency") String currency) {
      this.name = name;
      this.time = time;
      this.currency = currency;
      this.price = price;
   }

   public static ExchangeRate missingRate(String name, long time, String currency) {
      return new ExchangeRate(name, time, currency);
   }

   //explicit parameter instead of passing null, proguard did remove the null parameter otherwise
   private ExchangeRate(String name, long time, String currency) {
      this.name = name;
      this.time = time;
      this.currency = currency;
      this.price = null;
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("Name: ").append(name);
      sb.append(" time: ").append(new Date(time));
      sb.append(" currency: ").append(currency);
      sb.append(" price: ").append(price == null ? "<Not available>" : price);
      return sb.toString();
   }

   @Override
   public int hashCode() {
      return name.hashCode();
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == this) {
         return true;
      }
      if (!(obj instanceof ExchangeRate)) {
         return false;
      }
      ExchangeRate other = (ExchangeRate) obj;
      return other.time == time && other.name.equals(name) && other.currency.equals(currency);
   }

}
