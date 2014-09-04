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

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mrd.bitlib.model.OutPoint;
import com.mrd.bitlib.model.Transaction;
import com.mrd.bitlib.model.Transaction.TransactionParsingException;
import com.mrd.bitlib.model.TransactionOutput;
import com.mrd.bitlib.util.ByteReader;
import com.mrd.bitlib.util.HashUtils;
import com.mrd.bitlib.util.Sha256Hash;

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
