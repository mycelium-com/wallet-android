/*
 * Copyright 2013 Megion Research and Development GmbH
 *
 *  Licensed under the Microsoft Reference Source License (MS-RSL)
 *
 *  This license governs use of the accompanying software. If you use the software, you accept this license.
 *  If you do not accept the license, do not use the software.
 *
 *  1. Definitions
 *  The terms "reproduce," "reproduction," and "distribution" have the same meaning here as under U.S. copyright law.
 *  "You" means the licensee of the software.
 *  "Your company" means the company you worked for when you downloaded the software.
 *  "Reference use" means use of the software within your company as a reference, in read only form, for the sole purposes
 *  of debugging your products, maintaining your products, or enhancing the interoperability of your products with the
 *  software, and specifically excludes the right to distribute the software outside of your company.
 *  "Licensed patents" means any Licensor patent claims which read directly on the software as distributed by the Licensor
 *  under this license.
 *
 *  2. Grant of Rights
 *  (A) Copyright Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 *  worldwide, royalty-free copyright license to reproduce the software for reference use.
 *  (B) Patent Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 *  worldwide, royalty-free patent license under licensed patents for reference use.
 *
 *  3. Limitations
 *  (A) No Trademark License- This license does not grant you any rights to use the Licensor’s name, logo, or trademarks.
 *  (B) If you begin patent litigation against the Licensor over patents that you think may apply to the software
 *  (including a cross-claim or counterclaim in a lawsuit), your license to the software ends automatically.
 *  (C) The software is licensed "as-is." You bear the risk of using it. The Licensor gives no express warranties,
 *  guarantees or conditions. You may have additional consumer rights under your local laws which this license cannot
 *  change. To the extent permitted under your local laws, the Licensor excludes the implied warranties of merchantability,
 *  fitness for a particular purpose and non-infringement.
 *
 */

package com.mrd.mbwapi.util;

import java.util.HashSet;
import java.util.Set;

import com.mrd.bitlib.model.Address;
import com.mrd.mbwapi.api.TransactionSummary;
import com.mrd.mbwapi.api.TransactionSummary.Item;

public class TransactionSummaryUtils {

   private static final String[] EMPTY_STRING_ARRAY = new String[0];

   public enum TransactionType {
      ReceivedFromOthers, SentToOthers, SentToSelf
   };

   /**
    * Determine whether a transaction is a send, receive, or send to self
    * 
    * @param transaction
    *           The transaction
    * @param addresses
    *           The set of addresses owned by us \
    */
   public static TransactionType getTransactionType(TransactionSummary transaction, Set<Address> addresses) {
      if (areAllSendersMe(transaction, addresses)) {
         // Either sent to others or sent to self
         if (areAllReceiversMe(transaction, addresses)) {
            // Sent to self
            return TransactionType.SentToSelf;
         } else {
            // Sent to others
            return TransactionType.SentToOthers;
         }
      } else {
         // Received from others
         return TransactionType.ReceivedFromOthers;
      }
   }

   private static boolean areAllSendersMe(TransactionSummary transaction, Set<Address> addresses) {
      for (Item item : transaction.inputs) {
         if (!addresses.contains(item.address)) {
            return false;
         }
      }
      return true;
   }

   private static boolean areAllReceiversMe(TransactionSummary transaction, Set<Address> addresses) {
      for (Item item : transaction.outputs) {
         if (!addresses.contains(item.address)) {
            return false;
         }
      }
      return true;
   }

   /**
    * Calculate how the balance of a set of addresses is affected by this
    * transaction
    * 
    * @param transaction
    *           The transaction
    * @param addresses
    *           The addresses owned by us
    * @return the sum of satoshis that have been spent/received by our addresses
    *         in this transaction
    */
   public static long calculateBalanceChange(TransactionSummary transaction, Set<Address> addresses) {
      long in = 0, out = 0;
      for (Item item : transaction.inputs) {
         if (addresses.contains(item.address)) {
            in += item.value;
         }
      }
      for (Item item : transaction.outputs) {
         if (addresses.contains(item.address)) {
            out += item.value;
         }
      }
      return out - in;
   }

   public static String[] getReceiversNotMe(TransactionSummary transaction, Set<Address> addresses) {
      Set<String> receivers = new HashSet<String>();
      for (Item item : transaction.outputs) {
         if (!addresses.contains(item.address)) {
            receivers.add(item.address.toString());
         }
      }
      return receivers.toArray(EMPTY_STRING_ARRAY);
   }

   public static String[] getSenders(TransactionSummary transaction) {
      Set<String> senders = new HashSet<String>();
      for (Item item : transaction.inputs) {
         senders.add(item.address.toString());
      }
      return senders.toArray(EMPTY_STRING_ARRAY);
   }

}
