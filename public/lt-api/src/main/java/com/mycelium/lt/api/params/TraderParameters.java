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
import com.mrd.bitlib.crypto.PublicKey;
import com.mrd.bitlib.model.Address;

//todo rename TadeParameters / TraderParameters confusing
public class TraderParameters {
   @JsonProperty
   public String nickname;
   @JsonProperty
   public Address address;
   @JsonProperty
   public PublicKey publicKey;
   @JsonProperty
   public String sigSessionId;

   public TraderParameters(@JsonProperty("nickname") String nickname, @JsonProperty("address") Address address, @JsonProperty("publicKey") PublicKey publicKey,
         @JsonProperty("sigSessionId") String sigSessionId) {
      this.nickname = nickname;
      this.address = address;
      this.publicKey = publicKey;
      this.sigSessionId = sigSessionId;
   }

   @SuppressWarnings("unused")
   private TraderParameters() {
      // For Jackson
   }
}
