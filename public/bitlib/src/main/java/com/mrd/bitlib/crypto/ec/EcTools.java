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

/**
 * Parts of this code was extracted from the Java cryptography library from
 * www.bouncycastle.org.
 */
package com.mrd.bitlib.crypto.ec;

import java.math.BigInteger;

/**
 * Various tools for elliptic curves
 */
public class EcTools {

   /**
    * Get the length of the byte encoding of a field element
    */
   public static int getByteLength(int fieldSize) {
      return (fieldSize + 7) / 8;
   }

   /**
    * Get a big integer as an array of bytes of a specified length
    */
   public static byte[] integerToBytes(BigInteger s, int length) {
      byte[] bytes = s.toByteArray();

      if (length < bytes.length) {
         // The length is smaller than the byte representation. Truncate by
         // copying over the least significant bytes
         byte[] tmp = new byte[length];
         System.arraycopy(bytes, bytes.length - tmp.length, tmp, 0, tmp.length);
         return tmp;
      } else if (length > bytes.length) {
         // The length is larger than the byte representation. Copy over all
         // bytes and leave it prefixed by zeros.
         byte[] tmp = new byte[length];
         System.arraycopy(bytes, 0, tmp, tmp.length - bytes.length, bytes.length);
         return tmp;
      }
      return bytes;
   }

   /**
    * Multiply a point with a big integer
    */
   public static Point multiply(Point p, BigInteger k) {
      BigInteger e = k;
      BigInteger h = e.multiply(BigInteger.valueOf(3));

      Point neg = p.negate();
      Point R = p;

      for (int i = h.bitLength() - 2; i > 0; --i) {
         R = R.twice();

         boolean hBit = h.testBit(i);
         boolean eBit = e.testBit(i);

         if (hBit != eBit) {
            R = R.add(hBit ? p : neg);
         }
      }

      return R;
   }

   public static Point sumOfTwoMultiplies(Point P, BigInteger k, Point Q, BigInteger l) {
      int m = Math.max(k.bitLength(), l.bitLength());
      Point Z = P.add(Q);
      Point R = P.getCurve().getInfinity();

      for (int i = m - 1; i >= 0; --i) {
         R = R.twice();

         if (k.testBit(i)) {
            if (l.testBit(i)) {
               R = R.add(Z);
            } else {
               R = R.add(P);
            }
         } else {
            if (l.testBit(i)) {
               R = R.add(Q);
            }
         }
      }

      return R;
   }

   //ported from BitcoinJ
   public static Point decompressKey(BigInteger x, boolean firstBit) {
      int size = 1 + getByteLength(Parameters.curve.getFieldSize()); //hmmm..
      byte[] dest = integerToBytes(x, size);
      dest[0] = (byte) (firstBit ? 0x03 : 0x02); //hmm
      return Parameters.curve.decodePoint(dest);
   }
}
