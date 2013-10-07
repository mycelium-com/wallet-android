/*
 * Copyright 2013 Megion Research and Development GmbH
 *
 * Licensed under the Microsoft Reference Source License (MS-RSL)
 *
 * This license governs use of the accompanying software. If you use the software, you accept this license.
 * If you do not accept the license, do not use the software.
 *
 * 1. Definitions
 * The terms "reproduce," "reproduction," and "distribution" have the same meaning here as under U.S. copyright law.
 * "You" means the licensee of the software.
 * "Your company" means the company you worked for when you downloaded the software.
 * "Reference use" means use of the software within your company as a reference, in read only form, for the sole purposes
 * of debugging your products, maintaining your products, or enhancing the interoperability of your products with the
 * software, and specifically excludes the right to distribute the software outside of your company.
 * "Licensed patents" means any Licensor patent claims which read directly on the software as distributed by the Licensor
 * under this license.
 *
 * 2. Grant of Rights
 * (A) Copyright Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free copyright license to reproduce the software for reference use.
 * (B) Patent Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free patent license under licensed patents for reference use.
 *
 * 3. Limitations
 * (A) No Trademark License- This license does not grant you any rights to use the Licensor’s name, logo, or trademarks.
 * (B) If you begin patent litigation against the Licensor over patents that you think may apply to the software
 * (including a cross-claim or counterclaim in a lawsuit), your license to the software ends automatically.
 * (C) The software is licensed "as-is." You bear the risk of using it. The Licensor gives no express warranties,
 * guarantees or conditions. You may have additional consumer rights under your local laws which this license cannot
 * change. To the extent permitted under your local laws, the Licensor excludes the implied warranties of merchantability,
 * fitness for a particular purpose and non-infringement.
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
   public static int getByteLength(FieldElement fe) {
      return (fe.getFieldSize() + 7) / 8;
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

}
