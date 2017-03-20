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

import com.mrd.bitlib.crypto.ec.Parameters;
import com.mrd.bitlib.crypto.ec.Point;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.util.ByteReader;
import com.mrd.bitlib.util.HashUtils;
import com.mrd.bitlib.util.HexUtils;
import com.mrd.bitlib.util.Sha256Hash;

import java.io.Serializable;
import java.util.Arrays;


public class PublicKey implements Serializable {

   private static final long serialVersionUID = 1L;

   private final byte[] _pubKeyBytes;
   private byte[] _pubKeyHash;
   private Point _Q;

   public PublicKey(byte[] publicKeyBytes) {
      _pubKeyBytes = publicKeyBytes;
   }

   public Address toAddress(NetworkParameters networkParameters) {
      byte[] hashedPublicKey = getPublicKeyHash();
      return Address.fromStandardBytes(hashedPublicKey, networkParameters);
   }

   public byte[] getPublicKeyBytes() {
      return _pubKeyBytes;
   }

   public byte[] getPublicKeyHash() {
      if (_pubKeyHash == null) {
         _pubKeyHash = HashUtils.addressHash(_pubKeyBytes);
      }
      return _pubKeyHash;
   }

   @Override
   public int hashCode() {
      byte[] bytes = getPublicKeyHash();
      int hash = 0;
      for (int i = 0; i < bytes.length; i++) {
         hash = (hash << 8) + (bytes[i] & 0xff);
      }
      return hash;
   }

   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof PublicKey)) {
         return false;
      }
      PublicKey other = (PublicKey) obj;
      return Arrays.equals(getPublicKeyHash(), other.getPublicKeyHash());
   }

   @Override
   public String toString() {
      return HexUtils.toHex(_pubKeyBytes);
   }

   public boolean verifyStandardBitcoinSignature(Sha256Hash data, byte[] signature, boolean forceLowS) {
      // Decode parameters r and s
      ByteReader reader = new ByteReader(signature);
      Signature params = Signatures.decodeSignatureParameters(reader);
      if (params == null) {
         return false;
      }
      // Make sure that we have a hash type at the end
      if (reader.available() != 1) {
         return false;
      }
      if (forceLowS) {
         return Signatures.verifySignatureLowS(data.getBytes(), params, getQ());
      } else {
         return Signatures.verifySignature(data.getBytes(), params, getQ());
      }

   }

   // same as verifyStandardBitcoinSignature, but dont enforce the hash-type check
   public boolean verifyDerEncodedSignature(Sha256Hash data, byte[] signature){
      // Decode parameters r and s
      ByteReader reader = new ByteReader(signature);
      Signature params = Signatures.decodeSignatureParameters(reader);
      if (params == null) {
         return false;
      }
      return Signatures.verifySignature(data.getBytes(), params, getQ());
   }

   /**
    * Is this a compressed public key?
    */
   public boolean isCompressed() {
      return getQ().isCompressed();
   }

  public Point getQ() {
      if (_Q == null) {
         _Q = Parameters.curve.decodePoint(_pubKeyBytes);
      }
      return _Q;
   }

}
