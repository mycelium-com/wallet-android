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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;

public class PublicKeyRing implements IPublicKeyRing {
   private List<Address> _addresses;
   private Set<Address> _addressSet;
   private Map<Address, PublicKey> _publicKeys;

   public PublicKeyRing() {
      _addresses = new ArrayList<Address>();
      _addressSet = new HashSet<Address>();
      _publicKeys = new HashMap<Address, PublicKey>();
   }

   /**
    * Add a public key to the key ring.
    */
   public void addPublicKey(PublicKey key, NetworkParameters network) {
      Address address = key.toAddress(network);
      _addresses.add(address);
      _addressSet.add(address);
      _publicKeys.put(address, key);
   }

   /**
    * Add a public key and its corresponding Bitcoin address to the key ring.
    */
   public void addPublicKey(PublicKey key, Address address) {
      _addresses.add(address);
      _addressSet.add(address);
      _publicKeys.put(address, key);
   }

   @Override
   public PublicKey findPublicKeyByAddress(Address address) {
      return _publicKeys.get(address);
   }

   public List<Address> getAddresses() {
      return Collections.unmodifiableList(_addresses);
   }

   public Set<Address> getAddressSet() {
      return Collections.unmodifiableSet(_addressSet);
   }

}
