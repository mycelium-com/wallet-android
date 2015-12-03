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

package com.megiontechnologies;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * a core Bitcoin Value representation, caputuring many domain specific aspects
 * of it. introduced to reduce the ambiguity when dealing with double,
 * BigInteger, long, or even worse, integer representations
 * 
 * @author apetersson
 */
public final class Bitcoins implements Serializable {
   private static final long serialVersionUID = 1L;

   public static final long SATOSHIS_PER_BITCOIN = 100000000L;
   private static final BigDecimal SATOSHIS_PER_BITCOIN_BD = BigDecimal.valueOf(SATOSHIS_PER_BITCOIN);
   public static final long MAX_VALUE = 21000000 * SATOSHIS_PER_BITCOIN;
   // public static final String BITCOIN_SYMBOL = "\u0243"; // Ƀ
   // public static final String BITCOIN_SYMBOL = "\u0E3F"; // ฿
   public static final String BITCOIN_SYMBOL = "BTC"; // BTC

   private final long satoshis;

   /*    *//**
    * if used properly, also valueOf(input) should be provided ideally,
    * BitcoinJ would already output Bitcoins instead of BigInteger
    * 
    * @param output
    *           Object from BitcoiJ transaction
    * @return a value prepresentation of a Bitcoin domain object
    */
   /*
    * public static Bitcoins valueOf(TransactionOutput output) { return
    * Bitcoins.valueOf(output.getValue().longValue()); }
    */

   /**
    * @param btc
    *           double Value in full bitcoins. must be an exact represenatation
    * @return bitcoin value representation
    * @throws IllegalArgumentException
    *            if the given double value loses precision when converted to
    *            long
    */
   public static Bitcoins valueOf(double btc) {
      return valueOf(toLongExact(btc));
   }

    public static Bitcoins valueOf(String btc) {
        return Bitcoins.valueOf(new BigDecimal(btc).multiply(SATOSHIS_PER_BITCOIN_BD).longValueExact());
    }

    public static Bitcoins nearestValue(double v) {
      return new Bitcoins(Math.round(v * SATOSHIS_PER_BITCOIN));
   }

   public static Bitcoins nearestValue(BigDecimal bitcoinAmount) {
      BigDecimal satoshis = bitcoinAmount.multiply(SATOSHIS_PER_BITCOIN_BD);
      long satoshisExact = satoshis.setScale(0, RoundingMode.HALF_UP).longValueExact();
      return new Bitcoins(satoshisExact);
   }

   public static Bitcoins valueOf(long satoshis) {
      return new Bitcoins(satoshis);
   }

   private static long toLongExact(double origValue) {
      double satoshis = origValue * SATOSHIS_PER_BITCOIN; // possible loss of
                                                          // precision here
      long longSatoshis = Math.round(satoshis);
      return longSatoshis;
   }

   /**
    * XXX Jan: Commented out the below as this gives unnecessary runtime faults.
    * There may be rounding errors on the last decimals, and that is how life
    * is. The above simple conversion ois used instead.
    */

   // private static long toLongExact(double origValue) {
   // double satoshis = origValue * SATOSHIS_PER_BITCOIN; // possible loss of
   // // precision here?
   // long longSatoshis = Math.round(satoshis);
   // if (satoshis != (double) longSatoshis) {
   // double error = longSatoshis - satoshis;
   // throw new IllegalArgumentException("the given double value " + origValue
   // + " was not convertable to a precise value." + " error: " + error +
   // " satoshis");
   // }
   // return longSatoshis;
   // }

   private Bitcoins(long satoshis) {
      if (satoshis < 0)
         throw new IllegalArgumentException(String.format("Bitcoin values must be debt-free and positive, but was %s",
               satoshis));
      if (satoshis >= MAX_VALUE)
         throw new IllegalArgumentException(String.format(
               "Bitcoin values must be smaller than 21 Million BTC, but was %s", satoshis));
      this.satoshis = satoshis;
   }

   public BigDecimal multiply(BigDecimal pricePerBtc) {
      return toBigDecimal().multiply(BigDecimal.valueOf(satoshis));
   }

   protected Bitcoins parse(String input) {
      return Bitcoins.valueOf(input);
   }

   @Override
   public String toString() {
      // this could surely be implented faster without using BigDecimal. but it
      // is good enough for now.
      // this could be cached
      return toBigDecimal().toPlainString();
   }

   public String toString(int decimals) {
      // this could surely be implented faster without using BigDecimal. but it
      // is good enough for now.
      // this could be cached
      return toBigDecimal().setScale(decimals, RoundingMode.DOWN).toPlainString();
   }

   public BigDecimal toBigDecimal() {
      return BigDecimal.valueOf(satoshis).divide(SATOSHIS_PER_BITCOIN_BD);
   }

   @Override
   public int hashCode() {
      return (int) (satoshis ^ (satoshis >>> 32));
   }

   @Override
   public boolean equals(Object o) {
      if (this == o)
         return true;
      if (o == null || getClass() != o.getClass())
         return false;

      Bitcoins bitcoins = (Bitcoins) o;

      if (satoshis != bitcoins.satoshis)
         return false;

      return true;
   }

   public BigInteger toBigInteger() {
      return BigInteger.valueOf(satoshis);
   }

   public long getLongValue() {
      return satoshis;
   }

   public String toCurrencyString() {
      return new StringBuilder().append(BITCOIN_SYMBOL).append(' ').append(toString()).toString();
   }

   public String toCurrencyString(int decimals) {
      return new StringBuilder().append(BITCOIN_SYMBOL).append(' ').append(toString(decimals)).toString();
   }

   public Bitcoins roundToSignificantFigures(int n) {
      return Bitcoins.valueOf(roundToSignificantFigures(satoshis, n));
   }

   private static long roundToSignificantFigures(long num, int n) {
      if (num == 0) {
         return 0;
      }
      // todo check if these are equal, take LongMath
      // int d = LongMath.log10(Math.abs(num), RoundingMode.CEILING);
      final double d = Math.ceil(Math.log10(num < 0 ? -num : num));
      final int power = n - (int) d;

      final double magnitude = Math.pow(10, power);
      final long shifted = Math.round(num * magnitude);
      long ret = (long) (shifted / magnitude);
      return ret;
   }

}