package com.mycelium.wapi.api.lib;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import com.megiontechnologies.Bitcoins;

import java.io.Serializable;
import java.util.Date;


public class FeeEstimation implements Serializable {
   public static final FeeEstimation DEFAULT = new FeeEstimation(
         new FeeEstimationMap(
         new ImmutableMap.Builder<Integer, Bitcoins>()
               .put(1,  Bitcoins.valueOf(100000))   // 1mBtc/kB
               .put(3,  Bitcoins.valueOf( 20000))   // 0.2mBtc/kB
               .put(10, Bitcoins.valueOf( 10000))   // 0.1mBtc/kB
               .put(20, Bitcoins.valueOf(  1000))   // 0.01mBtc/kB
               .build())
            , new Date(0)
   );

   @JsonProperty
   final FeeEstimationMap feeForNBlocks;

   @JsonProperty
   final Date validFor;

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

      return feeForNBlocks.get(nBlocks);
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
}

