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
import com.mrd.bitlib.model.Transaction;
import com.mrd.bitlib.model.Transaction.TransactionParsingException;
import com.mrd.bitlib.model.TransactionOutput;
import com.mrd.bitlib.util.ByteReader;
import com.mrd.bitlib.util.HashUtils;
import com.mrd.bitlib.util.Sha256Hash;

import java.io.Serializable;
import java.util.Date;

public class TransactionEx implements Serializable, Comparable<TransactionEx> {
   private static final long serialVersionUID = 1L;

   @JsonProperty
   public final Sha256Hash txid;
   @JsonProperty
   public final int height; // -1 means unconfirmed
   @JsonProperty
   public final int time;
   @JsonProperty
   public final byte[] binary;

   public TransactionEx(@JsonProperty("txid") Sha256Hash txid, @JsonProperty("height") int height,
                        @JsonProperty("time") int time, @JsonProperty("binary") byte[] binary) {
      this.txid = txid;
      this.height = height;
      this.time = time;
      this.binary = binary;
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("txid:").append(txid).append(" height:").append(height).append(" byte-length: ").append(binary.length)
            .append(" time:").append(new Date(time * 1000L));
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
      TransactionEx other = (TransactionEx) obj;
      return txid.equals(other.txid);
   }

   public static TransactionEx fromUnconfirmedTransaction(Transaction t) {
      int now = (int) (System.currentTimeMillis() / 1000);
      return new TransactionEx(t.getHash(), -1, now, t.toBytes());
   }

   public static TransactionEx fromUnconfirmedTransaction(byte[] rawTransaction) {
      int now = (int) (System.currentTimeMillis() / 1000);
      Sha256Hash hash = HashUtils.doubleSha256(rawTransaction).reverse();
      return new TransactionEx(hash, -1, now, rawTransaction);
   }

   public static Transaction toTransaction(TransactionEx tex) {
      if (tex == null) {
         return null;
      }
      try {
         return Transaction.fromByteReader(new ByteReader(tex.binary));
      } catch (TransactionParsingException e) {
         return null;
      }
   }

   public static TransactionOutputEx getTransactionOutput(TransactionEx tex, int index) {
      if (index < 0) {
         return null;
      }
      Transaction t = toTransaction(tex);
      if (t == null) {
         return null;
      }
      if (index >= t.outputs.length) {
         return null;
      }
      TransactionOutput output = t.outputs[index];
      return new TransactionOutputEx(new OutPoint(tex.txid, index), tex.height, output.value,
            output.script.getScriptBytes(), t.isCoinbase());
   }

   public int calculateConfirmations(int blockHeight) {
      if (height == -1) {
         return 0;
      } else {
         return Math.max(0, blockHeight - height + 1);
      }
   }

   @Override
   public int compareTo(TransactionEx other) {
      // Make pending transaction have maximum height
      int myHeight = height == -1 ? Integer.MAX_VALUE : height;
      int otherHeight = other.height == -1 ? Integer.MAX_VALUE : other.height;

      if (myHeight < otherHeight) {
         return 1;
      } else if (myHeight > otherHeight) {
         return -1;
      } else {
         // sort by time
         if (time < other.time) {
            return 1;
         } else if (time > other.time) {
            return -1;
         }
         return 0;
      }
   }
}
