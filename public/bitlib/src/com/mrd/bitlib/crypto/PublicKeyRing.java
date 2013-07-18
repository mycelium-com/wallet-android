/*
 * Copyright 2013 Megion Research and Development GmbH
 *
 *  Licensed under the Microsoft Reference Source License (MS-RSL)
 *
 *  This license governs use of the accompanying software. If you use the software, you accept this license.
 *  If you do not accept the license, do not use the software.
 *
 *  1. Definitions
 *  The terms "reproduce," "reproduction," and "distribution" have the same meaning here as under U.S. copyright law.
 *  "You" means the licensee of the software.
 *  "Your company" means the company you worked for when you downloaded the software.
 *  "Reference use" means use of the software within your company as a reference, in read only form, for the sole purposes
 *  of debugging your products, maintaining your products, or enhancing the interoperability of your products with the
 *  software, and specifically excludes the right to distribute the software outside of your company.
 *  "Licensed patents" means any Licensor patent claims which read directly on the software as distributed by the Licensor
 *  under this license.
 *
 *  2. Grant of Rights
 *  (A) Copyright Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 *  worldwide, royalty-free copyright license to reproduce the software for reference use.
 *  (B) Patent Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 *  worldwide, royalty-free patent license under licensed patents for reference use.
 *
 *  3. Limitations
 *  (A) No Trademark License- This license does not grant you any rights to use the Licensor’s name, logo, or trademarks.
 *  (B) If you begin patent litigation against the Licensor over patents that you think may apply to the software
 *  (including a cross-claim or counterclaim in a lawsuit), your license to the software ends automatically.
 *  (C) The software is licensed "as-is." You bear the risk of using it. The Licensor gives no express warranties,
 *  guarantees or conditions. You may have additional consumer rights under your local laws which this license cannot
 *  change. To the extent permitted under your local laws, the Licensor excludes the implied warranties of merchantability,
 *  fitness for a particular purpose and non-infringement.
 *
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

public class PublicKeyRing {
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
      Address address = Address.fromStandardPublicKey(key, network);
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
