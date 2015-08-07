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

import com.google.common.base.Optional;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.util.Sha256Hash;


public class TransactionSummary implements Comparable<TransactionSummary> {
   public final Sha256Hash txid;
   public final long value;
   public final long time;
   public final int height;
   public final int confirmations;
   public final boolean isQueuedOutgoing;
   public final Optional<Address> destinationAddress;

   public TransactionSummary(Sha256Hash txid, long value, long time, int height, int confirmations, boolean isQueuedOutgoing, Optional<Address> destinationAddress) {
      this.txid = txid;
      this.value = value;
      this.time = time;
      this.height = height;
      this.confirmations = confirmations;
      this.isQueuedOutgoing = isQueuedOutgoing;
      this.destinationAddress = destinationAddress;
   }

   @Override
   public int compareTo(TransactionSummary other) {
      // First sort by confirmations
      if (confirmations < other.confirmations) {
         return 1;
      } else if (confirmations > other.confirmations) {
         return -1;
      } else {
         // Then sort by outgoing status
         if (isQueuedOutgoing != other.isQueuedOutgoing) {
            return isQueuedOutgoing ? 1 : -1;
         }
         // Finally sort by time
         if (time < other.time) {
            return 1;
         } else if (time > other.time) {
            return -1;
         }
         return 0;
      }
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
      if (!(obj instanceof TransactionSummary)) {
         return false;
      }
      TransactionSummary other = (TransactionSummary) obj;
      return other.txid.equals(this.txid);
   }

   public boolean canCancel() {
      return isQueuedOutgoing;
   }

   public boolean hasAddressBook() {
      return destinationAddress.isPresent();
   }

   public boolean hasDetails() {
      return true;
   }

   public boolean canCoinapult() {
      return false;
   }
}
