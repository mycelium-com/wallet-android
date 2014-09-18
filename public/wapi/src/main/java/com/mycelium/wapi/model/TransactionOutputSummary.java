package com.mycelium.wapi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.OutPoint;

import java.io.Serializable;

public class TransactionOutputSummary implements Comparable<TransactionOutputSummary>, Serializable {
   private static final long serialVersionUID = 1L;

   public final OutPoint outPoint;
   public final long value;
   public final int height;
   public final int confirmations;
   @JsonProperty
   public final Address address;

   public TransactionOutputSummary(OutPoint outPoint, long value,
                                   int height, int confirmations,
                                   Address address) {
      this.outPoint = outPoint;
      this.value = value;
      this.height = height;
      this.confirmations = confirmations;
      this.address = address;
   }

   @Override
   public int compareTo(TransactionOutputSummary other) {
      // First sort by confirmations
      if (confirmations < other.confirmations) {
         return 1;
      } else if (confirmations > other.confirmations) {
         return -1;
      } else {
         // Finally sort by value
         if (value < other.value) {
            return 1;
         } else if (value > other.value) {
            return -1;
         }
         return 0;
      }
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
      if (!(obj instanceof TransactionOutputSummary)) {
         return false;
      }
      TransactionOutputSummary other = (TransactionOutputSummary) obj;
      return other.outPoint.equals(this.outPoint);
   }
}
