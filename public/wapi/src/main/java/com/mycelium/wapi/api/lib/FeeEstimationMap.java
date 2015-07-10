package com.mycelium.wapi.api.lib;

import com.megiontechnologies.Bitcoins;

import java.util.HashMap;
import java.util.Map;

public class FeeEstimationMap extends HashMap<Integer, Bitcoins> {
   public FeeEstimationMap(Map<? extends Integer, ? extends Bitcoins> m) {
      super(m);
   }

   public FeeEstimationMap() {
   }

   public FeeEstimationMap(int initialCapacity) {
      super(initialCapacity);
   }

   public FeeEstimationMap(int initialCapacity, float loadFactor) {
      super(initialCapacity, loadFactor);
   }

   public Bitcoins put(Integer key, Bitcoins value, double correction) {
      Bitcoins valueAdjusted = Bitcoins.valueOf((long)((double)value.getLongValue() * correction)) ;
      return super.put(key, valueAdjusted);
   }
}
