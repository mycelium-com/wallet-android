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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mrd.bitlib.model.BitcoinAddress;
import com.mrd.bitlib.model.NetworkParameters;

public class PublicKeyRing implements IPublicKeyRing {
    private List<BitcoinAddress> _addresses;
    private Set<BitcoinAddress> _addressSet;
    private Map<BitcoinAddress, PublicKey> _publicKeys;

    public PublicKeyRing() {
        _addresses = new ArrayList<>();
        _addressSet = new HashSet<>();
        _publicKeys = new HashMap<>();
    }

    /**
     * Add a public key to the key ring.
     */
    public void addPublicKey(PublicKey key, NetworkParameters network) {
        Collection<BitcoinAddress> addresses = key.getAllSupportedAddresses(network).values();
        _addresses.addAll(addresses);
        _addressSet.addAll(addresses);
        for (BitcoinAddress address : addresses) {
            _publicKeys.put(address, key);
        }
    }

    /**
     * Add a public key and its corresponding Bitcoin address to the key ring.
     */
    public void addPublicKey(PublicKey key, BitcoinAddress address) {
        _addresses.add(address);
        _addressSet.add(address);
        _publicKeys.put(address, key);
    }

    @Override
    public PublicKey findPublicKeyByAddress(BitcoinAddress address) {
        return _publicKeys.get(address);
    }

    public List<BitcoinAddress> getAddresses() {
        return Collections.unmodifiableList(_addresses);
    }

    public Set<BitcoinAddress> getAddressSet() {
        return Collections.unmodifiableSet(_addressSet);
    }
}