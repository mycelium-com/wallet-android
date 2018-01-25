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

package com.mycelium.wapi.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mycelium.wapi.model.ExchangeRate;

import java.io.Serializable;

public class QueryExchangeRatesResponse implements Serializable {
   private static final long serialVersionUID = 1L;

   /**
    * The currency code for the currency the exchange rates it for
    */
   @JsonProperty
   public final String currency;

   /**
    * Information about Bitcoin exchange rates from zero or more exchanges for
    * the selected currency
    */
   @JsonProperty
   public final ExchangeRate[] exchangeRates;

   public QueryExchangeRatesResponse(@JsonProperty("currency") String currency, @JsonProperty("exchangeRates") ExchangeRate[] exchangeRates) {
      this.currency = currency;
      this.exchangeRates = exchangeRates;
   }
}
