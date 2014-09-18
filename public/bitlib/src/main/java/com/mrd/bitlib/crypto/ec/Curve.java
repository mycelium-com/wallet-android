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
