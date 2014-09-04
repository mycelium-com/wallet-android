package com.mycelium.wapi.model;

import com.mrd.bitlib.util.Sha256Hash;

public class TransactionSummary implements Comparable<TransactionSummary> {
   public final Sha256Hash txid;
   public final long value;
   public final long time;
   public final int height;
   public final int confirmations;
   public final boolean isOutgoing;

   public TransactionSummary(Sha256Hash txid, long value, long time, int height, int confirmations, boolean isOutgoing) {
      this.txid = txid;
      this.value = value;
      this.time = time;
      this.height = height;
      this.confirmations = confirmations;
      this.isOutgoing = isOutgoing;
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
         if (isOutgoing != other.isOutgoing) {
            return isOutgoing?1:-1;
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
}
