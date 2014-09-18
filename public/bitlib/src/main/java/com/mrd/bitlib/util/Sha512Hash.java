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

package com.mrd.bitlib.util;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Arrays;

/**
 * represents the result of a SHA256 hashing operation
 * prefer to use the static factory methods.
 */
public class Sha512Hash implements Serializable, Comparable<Sha512Hash> {
   private static final long serialVersionUID = 1L;

   public static final int HASH_LENGTH = 64;
   public static final Sha512Hash ZERO_HASH = of(new byte[HASH_LENGTH]);

   final private byte[] _bytes;
   private int _hash;

   public Sha512Hash(byte[] bytes) {
      Preconditions.checkArgument(bytes.length == HASH_LENGTH);
      this._bytes = bytes;
      _hash = -1;
   }

   /**
    * Takes 64 bytes and stores them as hash. does not actually hash, this is done in HashUtils
    * @param bytes to be stored
    */
   public static Sha512Hash of(byte[] bytes) {
      return new Sha512Hash(bytes);
   }

   public static Sha512Hash copyOf(byte[] bytes, int offset) {
      return new Sha512Hash(bytes, offset);
   }

   private Sha512Hash(byte[] bytes, int offset) {
      //defensive copy, since incoming bytes is of arbitrary length
      _bytes = new byte[HASH_LENGTH];
       System.arraycopy(bytes, offset, _bytes, 0, HASH_LENGTH);
      _hash = -1;
   }

   @Override
   public boolean equals(Object other) {
      if (other == this) {
         return true;
      }
      if (!(other instanceof Sha512Hash))
         return false;
      return Arrays.equals(_bytes, ((Sha512Hash) other)._bytes);
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
      return toHex();
   }

   public byte[] getBytes() {
      return _bytes;
   }

   @Override
   public int compareTo(Sha512Hash o) {
      for (int i = 0; i < HASH_LENGTH; i++) {
         byte myByte = _bytes[i];
         byte otherByte = o._bytes[i];

         final int compare = Ints.compare(myByte, otherByte);
         if (compare != 0)
            return compare;
      }
      return 0;
   }

   public Sha512Hash reverse() {
      return new Sha512Hash(BitUtils.reverseBytes(_bytes));
   }

   public int length() {
      return HASH_LENGTH;
   }

   public BigInteger toPositiveBigInteger() {
      return new BigInteger(1, _bytes);
   }

   public boolean startsWith(byte[] checksum) {
      Preconditions.checkArgument(checksum.length < HASH_LENGTH); //typcially 4
      for (int i = 0, checksumLength = checksum.length; i < checksumLength; i++) {
         if (_bytes[i] != checksum[i]) {
            return false;
         }
      }
      return true;
   }

   public byte[] firstFourBytes() {
      byte[] ret = new byte[4];
      System.arraycopy(_bytes, 0, ret, 0, 4);
      return ret;
   }

   public String toHex() {
      return HexUtils.toHex(_bytes);
   }

}
