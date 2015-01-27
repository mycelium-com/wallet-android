package com.mycelium.wapi.wallet;


public interface ExportableAccount {
   String getPrivateData(KeyCipher cipher) throws KeyCipher.InvalidKeyCipher;
   String getPublicData();
   boolean containsPrivateData();
}
