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
 * This code was extracted from the Java cryptography library from
 * www.bouncycastle.org. The code has been formatted to comply with the rest of
 * the formatting in this library.
 */
package com.mrd.bitlib.crypto.ec;

import java.io.Serializable;
import java.math.BigInteger;

/**
 * An elliptic curve
 */
public class Curve implements Serializable {

   private static final long serialVersionUID = 1L;

   private FieldElement _a;
   private FieldElement _b;
   private BigInteger _q;
   private Point _infinity;

   public Curve(BigInteger q, BigInteger a, BigInteger b) {
      this._q = q;
      this._a = fromBigInteger(a);
      this._b = fromBigInteger(b);
      this._infinity = new Point(this, null, null);
   }

   public FieldElement getA() {
      return _a;
   }

   public FieldElement getB() {
      return _b;
   }

   public BigInteger getQ() {
      return _q;
   }

   public Point getInfinity() {
      return _infinity;
   }

   public int getFieldSize() {
      return _q.bitLength();
   }

   public FieldElement fromBigInteger(BigInteger x) {
      return new FieldElement(this._q, x);
   }

   public Point createPoint(BigInteger x, BigInteger y, boolean withCompression) {
      return new Point(this, fromBigInteger(x), fromBigInteger(y), withCompression);
   }

   public Point decodePoint(byte[] encodedPoint) {
      Point p = null;
      // Switch on encoding type
      switch (encodedPoint[0]) {
      case 0x00:
         p = getInfinity();
         break;
      case 0x02:
      case 0x03:
         int ytilde = encodedPoint[0] & 1;
         byte[] i = new byte[encodedPoint.length - 1];
         System.arraycopy(encodedPoint, 1, i, 0, i.length);
         FieldElement x = new FieldElement(this._q, new BigInteger(1, i));
         FieldElement alpha = x.multiply(x.square().add(_a)).add(_b);
         FieldElement beta = alpha.sqrt();
         if (beta == null) {
            throw new RuntimeException("Invalid compression");
         }
         int bit0 = (beta.toBigInteger().testBit(0) ? 1 : 0);
         if (bit0 == ytilde) {
            p = new Point(this, x, beta, true);
         } else {
            p = new Point(this, x, new FieldElement(this._q, _q.subtract(beta.toBigInteger())), true);
         }
         break;
      case 0x04:
      case 0x06:
      case 0x07:
         byte[] xEnc = new byte[(encodedPoint.length - 1) / 2];
         byte[] yEnc = new byte[(encodedPoint.length - 1) / 2];
         System.arraycopy(encodedPoint, 1, xEnc, 0, xEnc.length);
         System.arraycopy(encodedPoint, xEnc.length + 1, yEnc, 0, yEnc.length);
         p = new Point(this, new FieldElement(this._q, new BigInteger(1, xEnc)), new FieldElement(this._q,
               new BigInteger(1, yEnc)));
         break;
      default:
         throw new RuntimeException("Invalid encoding 0x" + Integer.toString(encodedPoint[0], 16));
      }
      return p;
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == this) {
         return true;
      }
      if (!(obj instanceof Curve)) {
         return false;
      }
      Curve other = (Curve) obj;
      return this._q.equals(other._q) && _a.equals(other._a) && _b.equals(other._b);
   }

   @Override
   public int hashCode() {
      return _a.hashCode() ^ _b.hashCode() ^ _q.hashCode();
   }

}
