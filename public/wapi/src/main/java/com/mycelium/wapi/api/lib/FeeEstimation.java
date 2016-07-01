package com.mycelium.wapi.api.lib;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import com.megiontechnologies.Bitcoins;
import com.mrd.bitlib.model.Transaction;

import java.io.Serializable;
import java.util.Date;


public class FeeEstimation implements Serializable {
   public static final FeeEstimation DEFAULT = new FeeEstimation(
         new FeeEstimationMap(
         new ImmutableMap.Builder<Integer, Bitcoins>()
               .put(1,  Bitcoins.valueOf(100000))   // 1mBtc/kB
               .put(3,  Bitcoins.valueOf( 20000))   // 0.2mBtc/kB
               .put(10, Bitcoins.valueOf( 15000))   // 0.15mBtc/kB
               .put(20, Bitcoins.valueOf(  8000))   // 0.08mBtc/kB
               .build())
            , new Date(0)
   );

   @JsonProperty
   final FeeEstimationMap feeForNBlocks;

   @JsonProperty
   final Date validFor; // timestamp of this fee estimation

   public FeeEstimation(@JsonProperty("feeForNBlocks") FeeEstimationMap feeForNBlocks,
                        @JsonProperty("validFor") Date validFor) {
      this.feeForNBlocks = feeForNBlocks;
      this.validFor = validFor;
   }

   @JsonIgnore
   public Bitcoins getEstimation(int nBlocks){
      if (feeForNBlocks == null) {
         return null;
      }

      while (!feeForNBlocks.containsKey(nBlocks) && nBlocks>=0){
         nBlocks--;
      }

      if (nBlocks <= 0) {
         throw new IllegalArgumentException("nBlocks invalid");
      }

      Bitcoins bitcoins = feeForNBlocks.get(nBlocks);

      // check if we got a sane value, otherwise return a default value
      if (bitcoins.getLongValue() >= Transaction.MAX_MINER_FEE_PER_KB){
         return DEFAULT.getEstimation(nBlocks);
      } else {
         return bitcoins;
      }
   }

   @JsonIgnore
   public Date getValidFor(){
      return validFor;
   }

   @Override
   public String toString() {
      return "FeeEstimation: " +
            "feeForNBlocks=" + feeForNBlocks +
            '}';
   }

   /**
    * @param maxAge maximum age in millis until it is considered expired
    * @return true if the timestamp `validFor` of this FeeEstimation is older than maxAge. Else false.
    */
   public boolean isExpired(long maxAge) {
      final long feeAgeMillis = System.currentTimeMillis() - validFor.getTime();
      return feeAgeMillis > maxAge;
   }
}

