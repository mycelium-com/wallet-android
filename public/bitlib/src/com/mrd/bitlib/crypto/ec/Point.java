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
 * This class represents an elliptic curve point.
 */
public class Point implements Serializable {

   private static final long serialVersionUID = 1L;
   
   private Curve _curve;
   private FieldElement _x;
   private FieldElement _y;
   private boolean _compressed;

   public Point(Curve curve, FieldElement x, FieldElement y) {
      this(curve, x, y, false);
   }

   public Point(Curve curve, FieldElement x, FieldElement y, boolean compressed) {
      this._curve = curve;
      this._x = x;
      this._y = y;
      this._compressed = compressed;
   }

   public Curve getCurve() {
      return _curve;
   }

   public FieldElement getX() {
      return _x;
   }

   public FieldElement getY() {
      return _y;
   }

   public boolean isInfinity() {
      return _x == null && _y == null;
   }

   public boolean isCompressed() {
      return _compressed;
   }

   /**
    * return the field element encoded with point compression. (S 4.3.6)
    */
   public byte[] getEncoded() {
      if (this.isInfinity()) {
         return new byte[1];
      }

      int length = EcTools.getByteLength(_x);

      if (_compressed) {
         byte PC;

         if (this.getY().toBigInteger().testBit(0)) {
            PC = 0x03;
         } else {
            PC = 0x02;
         }

         byte[] X = EcTools.integerToBytes(this.getX().toBigInteger(), length);
         byte[] PO = new byte[X.length + 1];

         PO[0] = PC;
         System.arraycopy(X, 0, PO, 1, X.length);

         return PO;
      } else {
         byte[] X = EcTools.integerToBytes(this.getX().toBigInteger(), length);
         byte[] Y = EcTools.integerToBytes(this.getY().toBigInteger(), length);
         byte[] PO = new byte[X.length + Y.length + 1];

         PO[0] = 0x04;
         System.arraycopy(X, 0, PO, 1, X.length);
         System.arraycopy(Y, 0, PO, X.length + 1, Y.length);

         return PO;
      }
   }

   // B.3 pg 62
   public Point add(Point b) {
      if (this.isInfinity()) {
         return b;
      }

      if (b.isInfinity()) {
         return this;
      }

      // Check if b = this or b = -this
      if (this._x.equals(b._x)) {
         if (this._y.equals(b._y)) {
            // this = b, i.e. this must be doubled
            return this.twice();
         }

         // this = -b, i.e. the result is the point at infinity
         return this._curve.getInfinity();
      }

      FieldElement gamma = b._y.subtract(this._y).divide(b._x.subtract(this._x));

      FieldElement x3 = gamma.square().subtract(this._x).subtract(b._x);
      FieldElement y3 = gamma.multiply(this._x.subtract(x3)).subtract(this._y);

      return new Point(_curve, x3, y3);
   }

   // B.3 pg 62
   public Point twice() {
      if (this.isInfinity()) {
         // Twice identity element (point at infinity) is identity
         return this;
      }

      if (this._y.toBigInteger().signum() == 0) {
         // if y1 == 0, then (x1, y1) == (x1, -y1)
         // and hence this = -this and thus 2(x1, y1) == infinity
         return this._curve.getInfinity();
      }

      FieldElement TWO = this._curve.fromBigInteger(BigInteger.valueOf(2));
      FieldElement THREE = this._curve.fromBigInteger(BigInteger.valueOf(3));
      FieldElement gamma = this._x.square().multiply(THREE).add(_curve.getA()).divide(_y.multiply(TWO));

      FieldElement x3 = gamma.square().subtract(this._x.multiply(TWO));
      FieldElement y3 = gamma.multiply(this._x.subtract(x3)).subtract(this._y);

      return new Point(_curve, x3, y3, this._compressed);
   }

   // D.3.2 pg 102 (see Note:)
   public Point subtract(Point b) {
      if (b.isInfinity()) {
         return this;
      }

      // Add -b
      return add(b.negate());
   }

   public Point negate() {
      return new Point(_curve, this._x, this._y.negate(), this._compressed);
   }

   @Override
   public boolean equals(Object other) {
      if (other == this) {
         return true;
      }

      if (!(other instanceof Point)) {
         return false;
      }

      Point o = (Point) other;

      if (this.isInfinity()) {
         return o.isInfinity();
      }

      return _x.equals(o._x) && _y.equals(o._y);
   }

   @Override
   public int hashCode() {
      if (this.isInfinity()) {
         return 0;
      }

      return _x.hashCode() ^ _y.hashCode();
   }

}
