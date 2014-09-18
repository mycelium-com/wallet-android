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
import com.mrd.bitlib.model.OutPoint;

import java.io.Serializable;

public class TransactionOutputEx implements Serializable {
   private static final long serialVersionUID = 1L;

   @JsonProperty
   public final OutPoint outPoint;
   @JsonProperty
   public final int height; // -1 means unconfirmed
   @JsonProperty
   public final long value;
   @JsonProperty
   public final byte[] script;
   @JsonProperty
   public final boolean isCoinBase;

   public TransactionOutputEx(@JsonProperty("outPoint") OutPoint outPoint, @JsonProperty("height") int height,
                              @JsonProperty("value") long value, @JsonProperty("script") byte[] script,
                              @JsonProperty("isCoinBase") boolean isCoinBase) {
      this.outPoint = outPoint;
      this.height = height;
      this.value = value;
      this.script = script;
      this.isCoinBase = isCoinBase;
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("outPoint:").append(outPoint).append(" height:").append(height).append(" value: ").append(value)
            .append(" isCoinbase: ").append(isCoinBase).append(" scriptLength: ").append(script.length);
      return sb.toString();
   }

   @Override
   public int hashCode() {
      return outPoint.hashCode();
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == this) {
         return true;
      }
      if (!(obj instanceof TransactionOutputEx)) {
         return false;
      }
      TransactionOutputEx other = (TransactionOutputEx) obj;
      return outPoint.equals(other.outPoint);
   }

}
