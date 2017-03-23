package com.mycelium.wapi.wallet;


import com.google.common.base.Optional;

import java.io.Serializable;

public interface ExportableAccount {
   class Data implements Serializable {
      public final Optional<String> privateData;
      public final Optional<String> publicData;

      public Data(Optional<String> privateData, Optional<String> publicData) {
         this.privateData = privateData;
         this.publicData = publicData;
      }
   }

   Data getExportData(KeyCipher cipher);
}
