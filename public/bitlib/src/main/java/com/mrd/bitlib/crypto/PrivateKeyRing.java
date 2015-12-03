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

package com.mrd.bitlib.crypto;

import java.util.HashMap;
import java.util.Map;

import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;

public class PrivateKeyRing extends PublicKeyRing implements IPrivateKeyRing {

   private Map<PublicKey, PrivateKey> _privateKeys;

   public PrivateKeyRing() {
      _privateKeys = new HashMap<PublicKey, PrivateKey>();
   }

   /**
    * Add a private key to the key ring.
    */
   public void addPrivateKey(PrivateKey key, NetworkParameters network) {
      _privateKeys.put(key.getPublicKey(), key);
      addPublicKey(key.getPublicKey(), network);
   }

   /**
    * Add a private and public key pair along with the corresponding address to
    * the key ring.
    */
   public void addPrivateKey(PrivateKey privateKey, PublicKey publicKey, Address address) {
      _privateKeys.put(publicKey, privateKey);
      addPublicKey(publicKey, address);
   }

   /**
    * Find a Bitcoin signer by public key
    */
   @Override
   public BitcoinSigner findSignerByPublicKey(PublicKey publicKey) {
      return _privateKeys.get(publicKey);
   }

   /**
    * Find a KeyExporter by public key
    */
   public KeyExporter findKeyExporterByPublicKey(PublicKey publicKey) {
      PrivateKey key = _privateKeys.get(publicKey);
      if (key instanceof KeyExporter) {
         return (KeyExporter) key;
      }
      return null;
   }

}
