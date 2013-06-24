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

package com.mycelium.wallet;

import java.io.Serializable;

import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.util.HexUtils;

/**
 * Current Serialized Format: <HEX encoded 21 byte address>| <Bitcoin address
 * string>| <HEX encoded private key>| <HEX encoded public key>
 */
public class Record implements Serializable, Comparable<Record> {

   private static final long serialVersionUID = 1L;

   public InMemoryPrivateKey key;
   public Address address;
   public long timestamp;

   public Record(InMemoryPrivateKey key, long timestamp) {
      this.key = key;
      this.address = Address.fromStandardPublicKey(key.getPublicKey(), Constants.network);
      this.timestamp = timestamp;
   }

   public Record(InMemoryPrivateKey key, Address address, long timestamp) {
      this.key = key;
      this.address = address;
      this.timestamp = timestamp;
   }

   public Record(Address address, long timestamp) {
      this.address = address;
      this.timestamp = timestamp;
   }

   public void forgetPrivateKey() {
      key = null;
   }

   public boolean hasPrivateKey() {
      return key != null;
   }

   @Override
   public String toString() {
      return address.toString();
   }

   @Override
   public int compareTo(Record other) {
      
      // First sort on key / address
      if (hasPrivateKey() && !other.hasPrivateKey()) {
         return -1;
      } else if (!hasPrivateKey() && other.hasPrivateKey()) {
         return 1;
      }

      // Secondly sort on timestamp
      if (timestamp < other.timestamp) {
         return -1;
      } else if (timestamp > other.timestamp) {
         return 1;
      }
      
      // Thirdly sort on address
      return address.toString().compareTo(other.address.toString());

   }

   public String serialize() {

      // Serialization format is
      // "<timestamp-string>|<adress-bytes-hex>|<address-string>|<private-key-bytes-hex>|<public-key-bytes-hex>"

      String timestampString = Long.toString(timestamp);
      String addressBytesHex = HexUtils.toHex(address.getAllAddressBytes());
      String addressString = address.toString();
      String privateKeyBytesHex = hasPrivateKey() ? HexUtils.toHex(key.getPrivateKeyBytes()) : "";
      String publicKeyBytesHex = hasPrivateKey() ? HexUtils.toHex(key.getPublicKey().getPublicKeyBytes()) : "";

      StringBuilder sb = new StringBuilder();
      sb.append(timestampString).append('|');
      sb.append(addressString).append('|');
      sb.append(addressBytesHex).append('|');
      sb.append(privateKeyBytesHex).append('|');
      sb.append(publicKeyBytesHex);
      return sb.toString();
   }

   public static Record fromSerializedString(String serialized) {
      try {
         String[] split = chop(serialized, '|');
         if (split.length < 5) {
            throw new RuntimeException("Fatal error (1) while loading keys and addresses.");
         }

         String timestampString = split[0];
         String addressString = split[1];
         String addressBytesHex = split[2];
         String privateKeyBytesHex = split[3];
         String publicKeyBytesHex = split[4];

         if (timestampString.length() == 0) {
            throw new RuntimeException("Fatal error (2) while loading keys and addresses.");
         }
         // may throw
         long timestamp = Long.parseLong(timestampString);
         if (addressBytesHex.length() == 0 || addressString.length() == 0) {
            throw new RuntimeException("Fatal error (3) while loading keys and addresses.");
         }
         // may throw
         Address address = new Address(HexUtils.toBytes(addressBytesHex), addressString);
         InMemoryPrivateKey key = null;
         if (privateKeyBytesHex.length() != 0 && publicKeyBytesHex.length() != 0) {
            // may throw
            key = new InMemoryPrivateKey(HexUtils.toBytes(privateKeyBytesHex), HexUtils.toBytes(publicKeyBytesHex));
         }
         return new Record(key, address, timestamp);
      } catch (Exception e) {
         // Bail out all the way, as we do not want to risk saving on top of the
         // current records if we cannot parse them
         throw new RuntimeException("Fatal error (4) while loading keys and addresses.", e);
      }
   }

   private static String[] chop(String string, char separator) {
      // Count chunks
      char[] chars = string.toCharArray();
      int chunkCount = 1;
      for (int i = 0; i < chars.length; i++) {
         if (chars[i] == separator) {
            chunkCount++;
         }
      }

      // Get chunks
      String[] chunks = new String[chunkCount];
      int currentIndex = 0;
      int endIndex;
      int chunkIndex = 0;
      while ((endIndex = string.indexOf(separator, currentIndex)) != -1) {
         chunks[chunkIndex++] = string.substring(currentIndex, endIndex);
         currentIndex = endIndex + 1;
      }
      // insert last chunk
      chunks[chunkIndex] = string.substring(currentIndex);
      return chunks;
   }

   @Override
   public int hashCode() {
      return address.hashCode();
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == this) {
         return true;
      }
      if (!(obj instanceof Record)) {
         return false;
      }
      return this.address.equals(((Record) obj).address);
   }

}
