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
import java.util.List;

import com.google.common.base.Preconditions;
import com.mrd.bitlib.util.BitUtils;
import com.mrd.bitlib.util.ByteWriter;
import com.mrd.bitlib.util.HashUtils;

/**
 * Implementation of a Galois Field (2^8)
 */
public class Gf256 {

   public static final int DEFAULT_POLYNOMIAL = 0x11d;

   /**
    * A share of a secret
    */
   public static class Share {
      /**
       * The index of this share 0 < index < 256
       */
      public final byte index;

      /**
       * The content of this share
       */
      public final byte[] data;

      public Share(byte index, byte[] data) {
         this.index = index;
         this.data = data;
      }

   }

   private int[] _logTable = new int[256];
   private int[] _expTable = new int[256];

   private static final int INFINITY = 255;

   /*
    * Create a Galois Field with the default polynomial 0x11d
    */
   public Gf256() {
      this(DEFAULT_POLYNOMIAL);
   }

   /*
    * Create a Galois Field with the specified polynomial 0x11d
    */
   public Gf256(int polynomial) {
      // Initialize log and exponent tables by computing b = 2**i in GF
      // sequentially for all i from 0 to 254
      _logTable = new int[256];
      _expTable = new int[256];
      int b = 1; // 2**0
      for (int i = 0; i < 255; i++) {
         _logTable[b] = i;
         _expTable[i] = b;
         b <<= 1;
         if ((b & 0x100) > 0) {
            b ^= polynomial;
         }
      }
      _logTable[0] = (byte) INFINITY;
      _expTable[INFINITY] = 0;
      // Check that this polynomial really generates a GF by checking that we
      // are back to square one
      Preconditions.checkState(b == 1);
   }

   private final int log(int n) {
      return _logTable[n];
   }

   private final int exp(int n) {
      return _expTable[n];
   }

   /**
    * Addition. This is a simple X-or of two byte arrays
    */
   private final byte[] add(byte[] a, byte[] b) {
      Preconditions.checkState(a.length == b.length);
      byte[] c = new byte[a.length];
      for (int i = 0; i < a.length; i++) {
         c[i] = add(a[i], b[i]);
      }
      return c;
   }

   /**
    * Addition. This is a simple X-or of two bytes
    */
   private final byte add(byte a, byte b) {
      return (byte) (a ^ b);
   }

   /**
    * Substitution, same as addition
    */
   private final byte sub(byte a, byte b) {
      return add(a, b);
   }

   /**
    * Multiplication.
    */
   private byte[] mul(byte[] a, byte[] b) {
      Preconditions.checkState(a.length == b.length);
      byte[] c = new byte[a.length];
      for (int i = 0; i < a.length; i++) {
         c[i] = mul(a[i], b[i]);
      }
      return c;
   }

   /**
    * Multiplication.
    */
   private final byte mul(byte a, byte b) {
      if (a == 0 || b == 0) {
         return 0;
      } else {
         // The log of the product is the sum of the log of the multiplicands
         // modulo 255
         int lp = mod255(log(b2i(a)) + log(b2i(b)));
         return (byte) exp(lp);
      }
   }

   /**
    * Division.
    */
   private final byte div(byte a, byte b) {
      if (b == 0) {
         throw new RuntimeException("Division by zero");
      }
      if (a == 0) {
         return 0;
      } else {
         int lp = mod255(log(b2i(a)) - log(b2i(b)));
         return (byte) exp(lp);
      }
   }

   private int mod255(int n) {
      return n % 255 + (n < 0 ? 255 : 0);
   }

   private final int b2i(byte b) {
      return ((int) b) & 0xFF;
   }

   private byte[][] sha256Coefficients(byte[] secret, int m) {
      byte[][] res = new byte[m][];
      byte[] coeff = BitUtils.copyByteArray(secret);
      res[0] = coeff;
      for (int n = 1; n < m; n++) {
         ByteWriter writer = new ByteWriter(coeff.length + 32);
         for (int i = 0; i < coeff.length; i += 32) {
            int toHash = Math.min(32, coeff.length - i);
            writer.putBytes(HashUtils.sha256(coeff, i, toHash).getBytes());
         }
         coeff = BitUtils.copyOf(writer.toBytes(), coeff.length);
         res[n] = coeff;
      }
      return res;
   }

   private Share makeShare(byte x, byte[][] coeff) {
      Preconditions.checkArgument(x != 0);
      int q = coeff[0].length;
      byte[] s = coeff[0];

      byte xpow = 1;

      for (int i = 1; i < coeff.length; i++) {
         xpow = mul(xpow, x);
         s = add(s, mul(byteArrayOf(xpow, q), coeff[i]));
      }
      return new Share((byte) x, s);
   }

   /**
    * Combine a list of shares into a secret.
    * <p>
    * If the number of shares does not exactly match the original threshold that
    * the shares was created with, then the generated secret is not correct.
    * 
    * @param shares
    *           the shares to combine
    * @return the combined secret
    */
   public byte[] combineShares(List<Share> shares) {
      int m = shares.size();
      Preconditions.checkArgument(m > 0);
      int q = shares.get(0).data.length;

      byte n = 1;

      for (Share share : shares) {
         n = mul(n, share.index);
      }

      byte[] a = new byte[q];
      for (Share share : shares) {
         byte lc = div(n, share.index);
         for (Share otherShare : shares) {
            if (otherShare.index != share.index) {
               lc = div(lc, sub(otherShare.index, share.index));
            }
         }
         a = add(a, mul(share.data, byteArrayOf(lc, q)));
      }
      return a;
   }

   private static final byte[] byteArrayOf(byte b, int length) {
      byte[] result = new byte[length];
      for (int i = 0; i < length; i++) {
         result[i] = b;
      }
      return result;
   }

   /**
    * Shard a secret into a number of shares in such a way that only the
    * specified threshold number of shares can recreate the secret.
    * 
    * @param secret
    *           the secret to shard
    * @param threshold
    *           the number of shares needed to recreate the secret
    * @param shares
    *           the number of shares to create
    * @return a list of shares where only the specified threshold number of
    *         shares can recreate the secret
    */
   public List<Share> makeShares(byte[] secret, int threshold, int shares) {
      Preconditions.checkArgument(shares > 0, "Number of shares must be larger than zero");
      Preconditions.checkArgument(threshold <= shares,
            "Number of shares needed must be less than or equal to the number of shares");
      byte[][] coeff = sha256Coefficients(secret, threshold);
      List<Share> shareList = new ArrayList<Share>(shares);
      for (int i = 0; i < shares; i++) {
         shareList.add(makeShare((byte) (i + 1), coeff));
      }
      return shareList;
   }

}
