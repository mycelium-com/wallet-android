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

import java.io.Serializable;

public class Balance implements Serializable {
   private static final long serialVersionUID = 1L;

   /**
    * The sum of the unspent outputs which are confirmed and currently not spent
    * in pending transactions.
    */
   public final long confirmed;

   /**
    * The sum of the outputs which are being received as part of pending
    * transactions from foreign addresses.
    */
   public final long pendingReceiving;

   /**
    * The sum of outputs currently being sent from the address set.
    */
   public final long pendingSending;

   /**
    * The sum of the outputs being sent from the address set to itself
    */
   public final long pendingChange;

   /**
    * The time when this was last time synchronized with the blockchain
    */
   public final long updateTime;

   /**
    * The height of the block chain when this information was retrieved
    */
   public final int blockHeight;

   /**
    * Is the account in the process of synchronizing?
    * <p/>
    * This information allows a UI to display that the balance may not be final
    */
   public final boolean isSynchronizing;

   /**
    * Does the account that this balance came from allow spending generic zero confirmation outputs
    */
   public final boolean allowsZeroConfSpending;

   public Balance(long confirmed, long pendingReceiving, long pendingSending, long pendingChange, long updateTime,
                  int blockHeight, boolean isSynchronizing, boolean allowsZeroConfSpending) {
      this.confirmed = confirmed;
      this.pendingReceiving = pendingReceiving;
      this.pendingSending = pendingSending;
      this.pendingChange = pendingChange;
      this.updateTime = updateTime;
      this.blockHeight = blockHeight;
      this.isSynchronizing = isSynchronizing;
      this.allowsZeroConfSpending = allowsZeroConfSpending;
   }

   /**
    * Get the value to show as the balance in a UI
    */
   public long getSpendableBalance() {
      return allowsZeroConfSpending ? confirmed + pendingChange + pendingReceiving : confirmed + pendingChange;
   }

   /**
    * Get the value to show as receiving in a UI
    */
   public long getReceivingBalance() {
      return pendingReceiving;
   }

   /**
    * Get the value to show as sending in a UI
    */
   public long getSendingBalance() {
      return pendingSending - pendingChange;
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("Confirmed: ").append(confirmed);
      sb.append(" Receiving: ").append(pendingReceiving);
      sb.append(" Sending: ").append(pendingSending);
      sb.append(" Change: ").append(pendingChange);
      return sb.toString();
   }

   @Override
   public int hashCode() {
      return (int) (confirmed + pendingChange + pendingReceiving + pendingSending);
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == this) {
         return true;
      }
      // Cannot do instanceof for static inner classes
      if (obj == null || !getClass().equals(obj.getClass())) {
         return false;
      }
      Balance other = (Balance) obj;
      return confirmed == other.confirmed && pendingChange == other.pendingChange
            && pendingReceiving == other.pendingReceiving && pendingSending == other.pendingSending;
   }

}
