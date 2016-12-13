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

import java.io.Serializable;
import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mycelium.wapi.model.TransactionOutputEx;

public class QueryUnspentOutputsResponse implements Serializable {
   private static final long serialVersionUID = 1L;

   @JsonProperty
   public final int height;
   @JsonProperty
   public final Collection<TransactionOutputEx> unspent;

   public QueryUnspentOutputsResponse(@JsonProperty("height") int height,
                                      @JsonProperty("unspent") Collection<TransactionOutputEx> unspent) {
      this.height = height;
      this.unspent = unspent;
   }

}
