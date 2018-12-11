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

package com.mycelium.wapi.wallet.btc.single;

import com.google.common.base.Preconditions;
import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.crypto.PublicKey;
import com.mrd.bitlib.model.Address;
import com.mycelium.wapi.wallet.GenericAddress;
import com.mycelium.wapi.wallet.KeyCipher;
import com.mycelium.wapi.wallet.SecureKeyValueStore;

/**
 * A store for public and private keys by address' bytes
 */
public class PublicPrivateKeyStore {

   private SecureKeyValueStore _secureStorage;

   public PublicPrivateKeyStore(SecureKeyValueStore secureStorage) {
      _secureStorage = secureStorage;
   }

   public boolean isValidEncryptionKey(KeyCipher userCipher) {
      return _secureStorage.isValidEncryptionKey(userCipher);
   }

   public boolean hasPrivateKey(byte[] addressBytes) {
      return _secureStorage.hasCiphertextValue(addressBytes);
   }

   public boolean hasPrivateKey(GenericAddress address) {
      return hasPrivateKey(address.getBytes());
   }

   public void setPrivateKey(byte[] addressBytes, InMemoryPrivateKey privateKey, KeyCipher cipher) throws KeyCipher.InvalidKeyCipher {
      // Store the private key encrypted
      _secureStorage.encryptAndStoreValue(addressBytes, privateKey.getPrivateKeyBytes(), cipher);
      // Store the public key in plaintext
      _secureStorage.storePlaintextValue(addressBytes, privateKey.getPublicKey().getPublicKeyBytes());
   }

   public void setPrivateKey(GenericAddress address, InMemoryPrivateKey privateKey, KeyCipher cipher) throws KeyCipher.InvalidKeyCipher {
      setPrivateKey(address.getBytes(), privateKey, cipher);
   }

   public InMemoryPrivateKey getPrivateKey(byte[] addressBytes, KeyCipher cipher) throws KeyCipher.InvalidKeyCipher {
      byte[] pri = _secureStorage.getDecryptedValue(addressBytes, cipher);
      byte[] pub = _secureStorage.getPlaintextValue(addressBytes);
      if (pri == null || pub == null) {
         return null;
      }
      Preconditions.checkArgument(pri.length == 32);
      return new InMemoryPrivateKey(pri, pub);
   }

   public InMemoryPrivateKey getPrivateKey(GenericAddress address, KeyCipher cipher) throws KeyCipher.InvalidKeyCipher {
      return getPrivateKey(address.getBytes(), cipher);
   }

   public PublicKey getPublicKey(byte[] addressBytes) {
      byte[] value = _secureStorage.getPlaintextValue(addressBytes);
      if (value == null) {
         return null;
      }
      return new PublicKey(value);
   }

   public PublicKey getPublicKey(GenericAddress address) {
      return getPublicKey(address.getBytes());
   }

   public void forgetPrivateKey(byte[] addressBytes, KeyCipher cipher) throws KeyCipher.InvalidKeyCipher {
      _secureStorage.deleteEncryptedValue(addressBytes, cipher);
   }

   public void forgetPrivateKey(GenericAddress address, KeyCipher cipher) throws KeyCipher.InvalidKeyCipher {
      forgetPrivateKey(address.getBytes(), cipher);
   }


}
