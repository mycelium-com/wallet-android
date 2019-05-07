/*
 * Copyright 2013, 2014 Megion Research & Development GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mycelium.wapi.wallet.single;

import com.google.common.base.Preconditions;
import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.crypto.PublicKey;
import com.mrd.bitlib.model.Address;
import com.mycelium.wapi.wallet.KeyCipher;
import com.mycelium.wapi.wallet.SecureKeyValueStore;

/**
 * A store for public and private keys by address
 */
public class PublicPrivateKeyStore {

   private SecureKeyValueStore _secureStorage;

   public PublicPrivateKeyStore(SecureKeyValueStore secureStorage) {
      _secureStorage = secureStorage;
   }

   public boolean isValidEncryptionKey(KeyCipher userCipher) {
      return _secureStorage.isValidEncryptionKey(userCipher);
   }

   public boolean hasPrivateKey(Address address) {
      return _secureStorage.hasCiphertextValue(address.getAllAddressBytes());
   }

   public void setPrivateKey(Address address, InMemoryPrivateKey privateKey, KeyCipher cipher) throws KeyCipher.InvalidKeyCipher {
      // Store the private key encrypted
      _secureStorage.encryptAndStoreValue(address.getAllAddressBytes(), privateKey.getPrivateKeyBytes(), cipher);
      // Store the public key in plaintext
      _secureStorage.storePlaintextValue(address.getAllAddressBytes(), privateKey.getPublicKey().getPublicKeyBytes());
   }

   public InMemoryPrivateKey getPrivateKey(Address address, KeyCipher cipher) throws KeyCipher.InvalidKeyCipher {
      byte[] pri = _secureStorage.getDecryptedValue(address.getAllAddressBytes(), cipher);
      byte[] pub = _secureStorage.getPlaintextValue(address.getAllAddressBytes());
      if (pri == null || pub == null) {
         return null;
      }
      Preconditions.checkArgument(pri.length == 32);
      return new InMemoryPrivateKey(pri, pub);
   }

   public PublicKey getPublicKey(Address address) {
      byte[] value = _secureStorage.getPlaintextValue(address.getAllAddressBytes());
      if (value == null) {
         return null;
      }
      return new PublicKey(value);
   }

   public void forgetPrivateKey(Address address, KeyCipher cipher) throws KeyCipher.InvalidKeyCipher {
      _secureStorage.deleteEncryptedValue(address.getAllAddressBytes(), cipher);
   }
}
