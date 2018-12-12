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

package com.mrd.bitlib.model;

import com.google.common.base.Function;
import com.mrd.bitlib.bitcoinj.Base58;
import com.mrd.bitlib.model.hdpath.HdKeyPath;
import com.mrd.bitlib.util.BitUtils;
import com.mrd.bitlib.util.HashUtils;
import com.mrd.bitlib.util.Sha256Hash;

import java.io.Serializable;

public class Address implements Serializable, Comparable<Address> {
   private static final long serialVersionUID = 1L;
   public static final int NUM_ADDRESS_BYTES = 21;
   public static final Function<? super String,Address> FROM_STRING = new Function<String, Address>() {
      @Override
      public Address apply(String input) {
         return Address.fromString(input);
      }
   };

   private byte[] _bytes;
   private String _address;
   private Sha256Hash scriptHash;
   private HdKeyPath bip32Path;

   public static Address fromString(String address, NetworkParameters network) {
      Address addr = Address.fromString(address);
      if (addr == null) {
         return null;
      }
      if(!addr.isValidAddress(network)){
         return null;
      }
      return addr;
   }

   /**
    * @param address string representation of an address
    * @return an Address if address could be decoded with valid checksum and length of 21 bytes
    *         null else
    */
   public static Address fromString(String address) {
      if (address == null) {
         return null;
      }
      if (address.length() == 0) {
         return null;
      }
      try {
         return SegwitAddress.decode(address);
      } catch (SegwitAddress.SegwitAddressException e) {
         // this is not a SegWit address
      }

      byte[] bytes = Base58.decodeChecked(address);
      if (bytes == null || bytes.length != NUM_ADDRESS_BYTES) {
         return null;
      }
      return new Address(bytes);
   }

   public static Address fromP2SHBytes(byte[] bytes, NetworkParameters network) {
      if (bytes.length != 20) {
         return null;
      }
      byte[] all = new byte[NUM_ADDRESS_BYTES];
      all[0] = (byte) (network.getMultisigAddressHeader() & 0xFF);
      System.arraycopy(bytes, 0, all, 1, 20);
      return new Address(all);
   }

   public static Address fromStandardBytes(byte[] bytes, NetworkParameters network) {
      if (bytes.length != 20) {
         return null;
      }
      byte[] all = new byte[NUM_ADDRESS_BYTES];
      all[0] = (byte) (network.getStandardAddressHeader() & 0xFF);
      System.arraycopy(bytes, 0, all, 1, 20);
      return new Address(all);
   }

   /**
    * Construct a Bitcoin address from an array of bytes containing both the
    * address version and address bytes, but without the checksum (1 + 20 = 21
    * bytes).
    *
    * @param bytes containing the full address representation 1 + 20 bytes.
    */
   public Address(byte[] bytes) {
      _bytes = bytes;
      _address = null;
   }

   /**
    * Construct a Bitcoin address from an array of bytes and the string
    * representation of the address. The byte array contains both the address
    * version and address bytes, but without the checksum (1 + 20 = 21 bytes).
    * <p/>
    * Note: No attempt is made to verify that the byte array and string
    * representation match.
    *
    * @param bytes         containing the full address representation 1 + 20 bytes.
    * @param stringAddress the string representation of a Bitcoin address
    */
   public Address(byte[] bytes, String stringAddress) {
      _bytes = bytes;
      _address = stringAddress;
   }

   /**
    * Validate that an address is a valid address on the specified network
    */
   public boolean isValidAddress(NetworkParameters network) {
      byte version = getVersion();
      if (getAllAddressBytes().length != NUM_ADDRESS_BYTES) {
         return false;
      }
      return ((byte) (network.getStandardAddressHeader() & 0xFF)) == version
            || ((byte) (network.getMultisigAddressHeader() & 0xFF)) == version;
   }

   public boolean isP2SH(NetworkParameters network) {
      return getVersion() == (byte) (network.getMultisigAddressHeader() & 0xFF);
   }

   public byte getVersion() {
      return _bytes[0];
   }

   /**
    * Get the address as an array of bytes. The array contains the one byte
    * address type and the 20 address bytes, totaling 21 bytes.
    *
    * @return The address as an array of 21 bytes.
    */
   public byte[] getAllAddressBytes() {
      return _bytes;
   }

   /**
    * @return hash160 big endian bytes
    */
   public byte[] getTypeSpecificBytes() {
      byte[] result = new byte[20];
      System.arraycopy(_bytes, 1, result, 0, 20);
      return result;
   }

   @Override
   public String toString() {
      if (_address == null) {
         byte[] addressBytes = new byte[1 + 20 + 4];
         addressBytes[0] = _bytes[0];
         System.arraycopy(_bytes, 0, addressBytes, 0, NUM_ADDRESS_BYTES);
         Sha256Hash checkSum = HashUtils.doubleSha256(addressBytes, 0, NUM_ADDRESS_BYTES);
         System.arraycopy(checkSum.getBytes(), 0, addressBytes, NUM_ADDRESS_BYTES, 4);
         _address = Base58.encode(addressBytes);
      }
      return _address;
   }

   public AddressType getType() {
       if (isP2SH(getNetwork())) {
           return AddressType.P2SH_P2WPKH;
       } else {
           return AddressType.P2PKH;
       }
   }

   public String getShortAddress() {
      return this.getShortAddress(6);
   }

   public String getShortAddress(int showChars) {
      String addressString = toString();
      return addressString.substring(0, showChars) + "..." + addressString.substring(addressString.length() - showChars);
   }

   @Override
   public int hashCode() {
      return ((_bytes[16] & 0xFF) /* << 0 */) | ((_bytes[17] & 0xFF) << 8) | ((_bytes[18] & 0xFF) << 16)
            | ((_bytes[19] & 0xFF) << 24);
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == this) {
         return true;
      }
      if (!(obj instanceof Address)) {
         return false;
      }
      return BitUtils.areEqual(_bytes, ((Address) obj)._bytes);
   }

   public static Address getNullAddress(NetworkParameters network) {
      byte[] bytes = new byte[NUM_ADDRESS_BYTES];
      bytes[0] = (byte) (network.getStandardAddressHeader() & 0xFF);
      return new Address(bytes);
   }

   public static Address getNullAddress(NetworkParameters network, AddressType addressType) {
      byte[] bytes = new byte[NUM_ADDRESS_BYTES];

      if (addressType == null) {
         return getNullAddress(network);
      }

      switch (addressType) {
         case P2PKH:
            bytes[0] = (byte) (network.getStandardAddressHeader() & 0xFF);
            break;
         case P2WPKH:
            try {
               return new SegwitAddress(network, 0x00, BitUtils.copyOf(bytes, 20));
            } catch (SegwitAddress.SegwitAddressException ignore) {

            }
            break;
         case P2SH_P2WPKH:
            bytes[0] = (byte) (network.getMultisigAddressHeader() & 0xFF);
            break;
      }

      return new Address(bytes);
   }

   @Override
   public int compareTo(Address other) {
      // We sort on the actual address bytes.
      // We wish to achieve consistent sorting, the exact order is not
      // important.
      for (int i = 0; i < NUM_ADDRESS_BYTES; i++) {
         byte a = _bytes[i];
         byte b = other._bytes[i];
         if (a < b) {
            return -1;
         } else if (a > b) {
            return 1;
         }
      }
      return 0;
   }

   public String toMultiLineString() {
      String address = toString();
      return address.substring(0, 12) + "\r\n" +
              address.substring(12, 24) + "\r\n" +
              address.substring(24);
   }

   public String toDoubleLineString() {
      String address = toString();
      int splitIndex = address.length() / 2;
      return address.substring(0, splitIndex) + "\r\n" +
              address.substring(splitIndex);
   }

   public NetworkParameters getNetwork() {
      if (matchesNetwork(NetworkParameters.productionNetwork, getVersion())) {
         return NetworkParameters.productionNetwork;
      }
      if (matchesNetwork(NetworkParameters.testNetwork, getVersion())) {
         return NetworkParameters.testNetwork;
      }
      throw new IllegalStateException("unknown network");
   }

   private boolean matchesNetwork(NetworkParameters network, byte version) {
      return ((byte) (network.getStandardAddressHeader() & 0xFF)) == version || ((byte) (network.getMultisigAddressHeader() & 0xFF)) == version;
   }

   public Sha256Hash getScriptHash() {
      if (scriptHash == null) {
         byte[] scriptBytes;
         if (isP2SH(getNetwork())) {
            scriptBytes = new ScriptOutputP2SH(getTypeSpecificBytes()).getScriptBytes();
         } else {
            scriptBytes = new ScriptOutputP2PKH(getTypeSpecificBytes()).getScriptBytes();
         }
         scriptHash = HashUtils.sha256(scriptBytes).reverse();
      }
      return scriptHash;
   }

   public HdKeyPath getBip32Path() {
      return bip32Path;
   }

   public void setBip32Path(HdKeyPath bip32Path) {
      this.bip32Path = bip32Path;
   }
}
