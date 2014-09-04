/*
 * Copyright 2013 Megion Research and Development GmbH
 *
 * Licensed under the Microsoft Reference Source License (MS-RSL)
 *
 * This license governs use of the accompanying software. If you use the software, you accept this license.
 * If you do not accept the license, do not use the software.
 *
 * 1. Definitions
 * The terms "reproduce," "reproduction," and "distribution" have the same meaning here as under U.S. copyright law.
 * "You" means the licensee of the software.
 * "Your company" means the company you worked for when you downloaded the software.
 * "Reference use" means use of the software within your company as a reference, in read only form, for the sole purposes
 * of debugging your products, maintaining your products, or enhancing the interoperability of your products with the
 * software, and specifically excludes the right to distribute the software outside of your company.
 * "Licensed patents" means any Licensor patent claims which read directly on the software as distributed by the Licensor
 * under this license.
 *
 * 2. Grant of Rights
 * (A) Copyright Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free copyright license to reproduce the software for reference use.
 * (B) Patent Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free patent license under licensed patents for reference use.
 *
 * 3. Limitations
 * (A) No Trademark License- This license does not grant you any rights to use the Licensor’s name, logo, or trademarks.
 * (B) If you begin patent litigation against the Licensor over patents that you think may apply to the software
 * (including a cross-claim or counterclaim in a lawsuit), your license to the software ends automatically.
 * (C) The software is licensed "as-is." You bear the risk of using it. The Licensor gives no express warranties,
 * guarantees or conditions. You may have additional consumer rights under your local laws which this license cannot
 * change. To the extent permitted under your local laws, the Licensor excludes the implied warranties of merchantability,
 * fitness for a particular purpose and non-infringement.
 */

package com.mycelium.wapi.model;

public class Balance {

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
    * <p>
    * This information allows a UI to display that the balance may not be final
    */
   public final boolean isSynchronizing;

   public Balance(long confirmed, long pendingReceiving, long pendingSending, long pendingChange, long updateTime,
         int blockHeight, boolean isSynchronizing) {
      this.confirmed = confirmed;
      this.pendingReceiving = pendingReceiving;
      this.pendingSending = pendingSending;
      this.pendingChange = pendingChange;
      this.updateTime = updateTime;
      this.blockHeight = blockHeight;
      this.isSynchronizing = isSynchronizing;
   }

   /**
    * Get the value to show as the balance in a UI
    */
   public long getSpendableBalance() {
      return confirmed + pendingChange;
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
            && pendingReceiving == other.pendingReceiving && pendingSending == other.pendingSending
            && blockHeight == other.blockHeight;
   }

}
