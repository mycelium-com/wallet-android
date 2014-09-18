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
import java.util.Random;

/**
 * This class represents an elliptic field element.
 */
public class FieldElement implements Serializable {

   private static final long serialVersionUID = 1L;

   private static final BigInteger TWO = BigInteger.valueOf(2);

   private BigInteger _x;
   private BigInteger _q;

   public FieldElement(BigInteger q, BigInteger x) {
      this._x = x;
      if (x.compareTo(q) >= 0) {
         throw new IllegalArgumentException("x value too large in field element");
      }
      this._q = q;
   }

   public BigInteger toBigInteger() {
      return _x;
   }

   public int getFieldSize() {
      return _q.bitLength();
   }

   public BigInteger getQ() {
      return _q;
   }

   public FieldElement add(FieldElement b) {
      return new FieldElement(_q, _x.add(b.toBigInteger()).mod(_q));
   }

   public FieldElement subtract(FieldElement b) {
      return new FieldElement(_q, _x.subtract(b.toBigInteger()).mod(_q));
   }

   public FieldElement multiply(FieldElement b) {
      return new FieldElement(_q, _x.multiply(b.toBigInteger()).mod(_q));
   }

   public FieldElement divide(FieldElement b) {
      return new FieldElement(_q, _x.multiply(b.toBigInteger().modInverse(_q)).mod(_q));
   }

   public FieldElement negate() {
      return new FieldElement(_q, _x.negate().mod(_q));
   }

   public FieldElement square() {
      return new FieldElement(_q, _x.multiply(_x).mod(_q));
   }

   public FieldElement invert() {
      return new FieldElement(_q, _x.modInverse(_q));
   }

   @Override
   public String toString() {
      return this.toBigInteger().toString(2);
   }

   public FieldElement sqrt() {
      if (!_q.testBit(0)) {
         throw new RuntimeException("not done yet");
      }

      if (_q.testBit(1)) {
         // z = g^(u+1) + p, p = 4u + 3
         FieldElement z = new FieldElement(_q, _x.modPow(_q.shiftRight(2).add(BigInteger.ONE), _q));

         return z.square().equals(this) ? z : null;
      }

      // p mod 4 == 1
      BigInteger qMinusOne = _q.subtract(BigInteger.ONE);

      BigInteger legendreExponent = qMinusOne.shiftRight(1);
      if (!(_x.modPow(legendreExponent, _q).equals(BigInteger.ONE))) {
         return null;
      }

      BigInteger u = qMinusOne.shiftRight(2);
      BigInteger k = u.shiftLeft(1).add(BigInteger.ONE);

      BigInteger Q = this._x;
      BigInteger fourQ = Q.shiftLeft(2).mod(_q);

      BigInteger U, V;
      Random rand = new Random();
      do {
         BigInteger P;
         do {
            P = new BigInteger(_q.bitLength(), rand);
         } while (P.compareTo(_q) >= 0
               || !(P.multiply(P).subtract(fourQ).modPow(legendreExponent, _q).equals(qMinusOne)));

         BigInteger[] result = lucasSequence(_q, P, Q, k);
         U = result[0];
         V = result[1];

         if (V.multiply(V).mod(_q).equals(fourQ)) {
            // Integer division by 2, mod q
            if (V.testBit(0)) {
               V = V.add(_q);
            }

            V = V.shiftRight(1);

            return new FieldElement(_q, V);
         }
      } while (U.equals(BigInteger.ONE) || U.equals(qMinusOne));

      return null;

   }

   private static BigInteger[] lucasSequence(BigInteger p, BigInteger P, BigInteger Q, BigInteger k) {
      int n = k.bitLength();
      int s = k.getLowestSetBit();

      BigInteger Uh = BigInteger.ONE;
      BigInteger Vl = TWO;
      BigInteger Vh = P;
      BigInteger Ql = BigInteger.ONE;
      BigInteger Qh = BigInteger.ONE;

      for (int j = n - 1; j >= s + 1; --j) {
         Ql = Ql.multiply(Qh).mod(p);

         if (k.testBit(j)) {
            Qh = Ql.multiply(Q).mod(p);
            Uh = Uh.multiply(Vh).mod(p);
            Vl = Vh.multiply(Vl).subtract(P.multiply(Ql)).mod(p);
            Vh = Vh.multiply(Vh).subtract(Qh.shiftLeft(1)).mod(p);
         } else {
            Qh = Ql;
            Uh = Uh.multiply(Vl).subtract(Ql).mod(p);
            Vh = Vh.multiply(Vl).subtract(P.multiply(Ql)).mod(p);
            Vl = Vl.multiply(Vl).subtract(Ql.shiftLeft(1)).mod(p);
         }
      }

      Ql = Ql.multiply(Qh).mod(p);
      Qh = Ql.multiply(Q).mod(p);
      Uh = Uh.multiply(Vl).subtract(Ql).mod(p);
      Vl = Vh.multiply(Vl).subtract(P.multiply(Ql)).mod(p);
      Ql = Ql.multiply(Qh).mod(p);

      for (int j = 1; j <= s; ++j) {
         Uh = Uh.multiply(Vl).mod(p);
         Vl = Vl.multiply(Vl).subtract(Ql.shiftLeft(1)).mod(p);
         Ql = Ql.multiply(Ql).mod(p);
      }

      return new BigInteger[] { Uh, Vl };
   }

   public boolean equals(Object other) {
      if (other == this) {
         return true;
      }

      if (!(other instanceof FieldElement)) {
         return false;
      }

      FieldElement o = (FieldElement) other;
      return _q.equals(o._q) && _x.equals(o._x);
   }

   public int hashCode() {
      return _q.hashCode() ^ _x.hashCode();
   }
}
