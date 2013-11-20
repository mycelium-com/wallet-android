/*
 * Copyright 2013 Megion Research & Development GmbH
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

import java.io.Serializable;
import java.math.BigInteger;

import com.mrd.bitlib.util.ByteWriter;

public abstract class PrivateKey implements BitcoinSigner, Serializable {

   private static final long serialVersionUID = 1L;

   public abstract PublicKey getPublicKey();

   @Override
   public byte[] makeStandardBitcoinSignature(byte[] transactionSigningHash, RandomSource randomSource) {
      byte[] signature = signMessage(transactionSigningHash, randomSource);
      ByteWriter writer = new ByteWriter(1024);
      // Add signature
      writer.putBytes(signature);
      // Add hash type
      writer.put((byte) ((0 + 1) | 0));
      return writer.toBytes();
   }

   protected byte[] signMessage(byte[] message, RandomSource randomSource) {
      BigInteger[] signature = generateSignature(message, randomSource);
      // Write DER encoding of signature
      ByteWriter writer = new ByteWriter(1024);
      // Write tag
      writer.put((byte) 0x30);
      // Write total length
      byte[] s1 = signature[0].toByteArray();
      byte[] s2 = signature[1].toByteArray();
      int totalLength = 2 + s1.length + 2 + s2.length;
      if (totalLength > 127) {
         // We assume that the total length never goes beyond a 1-byte
         // representation
         throw new RuntimeException("Unsupported signature length: " + totalLength);
      }
      writer.put((byte) (totalLength & 0xFF));
      // Write type
      writer.put((byte) 0x02);
      // We assume that the length never goes beyond a 1-byte representation
      writer.put((byte) (s1.length & 0xFF));
      // Write bytes
      writer.putBytes(s1);
      // Write type
      writer.put((byte) 0x02);
      // We assume that the length never goes beyond a 1-byte representation
      writer.put((byte) (s2.length & 0xFF));
      // Write bytes
      writer.putBytes(s2);
      return writer.toBytes();
   }

   protected abstract BigInteger[] generateSignature(byte[] message, RandomSource randomSource);

   @Override
   public int hashCode() {
      return getPublicKey().hashCode();
   }

   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof PrivateKey)) {
         return false;
      }
      PrivateKey other = (PrivateKey) obj;
      return getPublicKey().equals(other.getPublicKey());
   }

}
