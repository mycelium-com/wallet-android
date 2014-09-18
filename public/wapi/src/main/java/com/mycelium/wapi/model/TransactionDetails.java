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

import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.util.Sha256Hash;

import java.io.Serializable;

public class TransactionDetails implements Comparable<TransactionDetails>, Serializable {
   private static final long serialVersionUID = 1L;

   public static class Item implements Serializable {
      private static final long serialVersionUID = 1L;
      public final Address address;
      public final long value;

      public Item(Address address, long value) {
         this.address = address;
         this.value = value;
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

}
