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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mrd.bitlib.util.Sha256Hash;

import java.io.Serializable;

public class TransactionStatus implements Serializable {
   private static final long serialVersionUID = 1L;

   @JsonProperty
   public final Sha256Hash txid;
   @JsonProperty
   public final boolean found;
   @JsonProperty
   public final int height; // -1 means unconfirmed
   @JsonProperty
   public final int time;

   @JsonCreator
   public TransactionStatus(@JsonProperty("txid") Sha256Hash txid, @JsonProperty("found") boolean found,
                            @JsonProperty("height") int height, @JsonProperty("time") int time) {
      this.txid = txid;
      this.found = found;
      this.height = height;
      this.time = time;
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("txid:").append(txid).append(" found:").append(found).append(" height:").append(height);
      return sb.toString();
   }

   @Override
   public int hashCode() {
      return txid.hashCode();
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == this) {
         return true;
      }
      if (!(obj instanceof TransactionStatus)) {
         return false;
      }
      TransactionStatus other = (TransactionStatus) obj;
      return txid.equals(other.txid);
   }

}
