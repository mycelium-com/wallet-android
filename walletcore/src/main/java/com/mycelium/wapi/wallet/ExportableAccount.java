package com.mycelium.wapi.wallet;


import com.google.common.base.Optional;
import com.mrd.bitlib.crypto.BipDerivationType;

import java.io.Serializable;
import java.util.Map;

public interface ExportableAccount {
   class Data implements Serializable {
      public final Optional<String> privateData;
      private Map<BipDerivationType, String> privateDataMap;
      public final Optional<String> publicData;
      private Map<BipDerivationType, String> publicDataMap;

      public Data(Optional<String> privateData, Optional<String> publicData) {
         this.privateData = privateData;
         this.publicData = publicData;
      }

      public Data(Map<BipDerivationType, String> privateDataMap, Map<BipDerivationType, String> publicDataMap) {
         this.privateData = Optional.of(privateDataMap.get(privateDataMap.keySet().iterator().next()));
         this.privateDataMap  = privateDataMap;
         this.publicData = Optional.of(publicDataMap.get(publicDataMap.keySet().iterator().next()));
         this.publicDataMap = publicDataMap;
      }

      public Map<BipDerivationType, String> getPrivateDataMap() {
         return privateDataMap;
      }

      public Map<BipDerivationType, String> getPublicDataMap() {
         return publicDataMap;
      }
   }

   Data getExportData(KeyCipher cipher);
}
