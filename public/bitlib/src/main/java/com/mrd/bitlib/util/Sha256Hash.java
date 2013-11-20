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

package com.mrd.bitlib.util;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import com.google.common.primitives.Ints;

public class Sha256Hash implements Serializable, Comparable<Sha256Hash> {
   private static final long serialVersionUID = 1L;

   public static final Sha256Hash ZERO_HASH = new Sha256Hash();
   public static final int HASH_LENGTH = 32;

   final private byte[] _bytes;
   private int _hash;

   private Sha256Hash() {
      this._bytes = new byte[32];
      _hash = -1;
   }

   public Sha256Hash(byte[] bytes) {
      this._bytes = bytes;
      _hash = -1;
   }

   public Sha256Hash(byte[] bytes, boolean reverse) {
      if (reverse) {
         this._bytes = BitUtils.reverseBytes(bytes);
      } else {
         this._bytes = bytes;

      }
      _hash = -1;
   }

   public Sha256Hash(byte[] bytes, int offset, boolean reverse) {
      _bytes = new byte[32];
      if (reverse) {
         // Copy 32 byte hash from offset and reverse byte order
         for (int i = 0; i < _bytes.length; i++) {
            _bytes[i] = bytes[offset + 32 - 1 - i];
         }
      } else {
         System.arraycopy(bytes, offset, _bytes, 0, 32);
      }
      _hash = -1;
   }

   public Sha256Hash(ByteBuffer buf, boolean reverse) {
      byte[] bytes = new byte[32];
      buf.get(bytes, 0, 32);
      if (reverse) {
         this._bytes = BitUtils.reverseBytes(bytes);
      } else {
         this._bytes = bytes;
      }
      _hash = -1;
   }

   /**
    * @param contents
    *           bytes of f.ex. a brainwallet
    * @return a sha256 hash out of any byte array, by applying SHA-256 once
    */
   public static Sha256Hash create(byte[] contents) {
      try {
         MessageDigest digest = MessageDigest.getInstance("SHA-256");
         return new Sha256Hash(digest.digest(contents));
      } catch (NoSuchAlgorithmException e) {
         throw new RuntimeException(e); // Cannot happen.
      }
   }

   @Override
   public boolean equals(Object other) {
      if (other == this) {
         return true;
      }
      if (!(other instanceof Sha256Hash))
         return false;
      return Arrays.equals(_bytes, ((Sha256Hash) other)._bytes);
   }

   @Override
   public int hashCode() {
      if (_hash == -1) {
         final int offset = _bytes.length - 4;
         _hash = 0;
         for (int i = 0; i < 4; i++) {
            _hash <<= 8;
            _hash |= (((int) _bytes[offset + i]) & 0xFF);
         }
      }
      return _hash;
   }

   @Override
   public String toString() {
      return HexUtils.toHex(_bytes);
   }

   public byte[] getBytes() {
      return _bytes;
   }

   public void toByteBuffer(ByteBuffer buf, boolean reverse) {
      if (reverse) {
         buf.put(BitUtils.reverseBytes(_bytes));
      } else {
         buf.put(_bytes);
      }
   }

   public Sha256Hash duplicate() {
      return new Sha256Hash(_bytes);
   }

   @Override
   public int compareTo(Sha256Hash o) {
      for (int i = 0; i < HASH_LENGTH; i++) {
         byte myByte = _bytes[i];
         byte otherByte = o._bytes[i];

         final int compare = Ints.compare(myByte, otherByte);
         if (compare != 0)
            return compare;
      }
      return 0;
   }
}
