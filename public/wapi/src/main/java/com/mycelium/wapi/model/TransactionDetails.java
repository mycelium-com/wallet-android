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

import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.util.Sha256Hash;

import java.io.Serializable;

public class TransactionDetails implements Comparable<TransactionDetails>, Serializable {
   private static final long serialVersionUID = 1L;

   public static class Item implements Serializable {
      private static final long serialVersionUID = 1L;
      public final Address address;
      public final long value;
      public final boolean isCoinbase;

      public Item(Address address, long value, boolean isCoinbase) {
         this.address = address;
         this.value = value;
         this.isCoinbase = isCoinbase;
      }
   }

   public final Sha256Hash hash;
   public final int height;
   public final int time;
   public final Item[] inputs;
   public final Item[] outputs;

   public TransactionDetails(Sha256Hash hash, int height, int time, Item[] inputs, Item[] outputs) {
      this.hash = hash;
      this.height = height;
      this.time = time;
      this.inputs = inputs;
      this.outputs = outputs;
   }

   /**
    * Calculate the number of confirmations on this transaction from the current
    * block height.
    */
   public int calculateConfirmations(int currentHeight) {
      if (height == -1) {
         return 0;
      } else {
         return currentHeight - height + 1;
      }
   }

   @Override
   public int hashCode() {
      return hash.hashCode();
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == this) {
         return true;
      }
      if (!(obj instanceof TransactionDetails)) {
         return false;
      }
      TransactionDetails other = (TransactionDetails) obj;
      return other.hash.equals(this.hash);
   }


   @Override
   public int compareTo(TransactionDetails other) {
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


   protected NetworkParameters getNetwork(){
      if (inputs.length>0){
         return inputs[0].address.getNetwork();
      }else if (outputs.length>0){
         return inputs[0].address.getNetwork();
      }else{
         throw new RuntimeException("Transaction without inputs or outputs - unable to determine network");
      }
   }

   public String toString(){
      return hash.toString();
   }

}
