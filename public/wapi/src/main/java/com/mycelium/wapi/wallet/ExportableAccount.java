package com.mycelium.wapi.wallet;


public interface ExportableAccount {
   String getPrivateData(KeyCipher cipher) throws KeyCipher.InvalidKeyCipher;
   String getPublicData();

   // does this account store locally private date (xPriv, PrivateKey, ...)
   boolean containsPrivateData();
}
