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
 * a core Bitcoin Cash Value representation, caputuring many domain specific aspects
 * of it. introduced to reduce the ambiguity when dealing with double,
 * BigInteger, long, or even worse, integer representations
 * 
 * @author apetersson
 */
public final class BitcoinCash implements Serializable {
   private static final long serialVersionUID = 1L;

   public static final long SATOSHIS_PER_BITCOIN_CASH = 100000000L;
   private static final BigDecimal SATOSHIS_PER_BITCOIN_CASH_BD = BigDecimal.valueOf(SATOSHIS_PER_BITCOIN_CASH);
   public static final long MAX_VALUE = 21000000 * SATOSHIS_PER_BITCOIN_CASH;
   public static final String BITCOIN_CASH_SYMBOL = "BCH"; // BTC

   private final long satoshis;

   /**
    * @param bch
    *           double Value in full bitcoins. must be an exact represenatation
    * @return bitcoin cash value representation
    * @throws IllegalArgumentException
    *            if the given double value loses precision when converted to
    *            long
    */
   public static BitcoinCash valueOf(double bch) {
      return valueOf(toLongExact(bch));
   }

    public static BitcoinCash valueOf(String bch) {
        return BitcoinCash.valueOf(new BigDecimal(bch).multiply(SATOSHIS_PER_BITCOIN_CASH_BD).longValueExact());
    }

    public static BitcoinCash nearestValue(double v) {
      return new BitcoinCash(Math.round(v * SATOSHIS_PER_BITCOIN_CASH));
   }

   public static BitcoinCash nearestValue(BigDecimal bitcoinAmount) {
      BigDecimal satoshis = bitcoinAmount.multiply(SATOSHIS_PER_BITCOIN_CASH_BD);
      long satoshisExact = satoshis.setScale(0, RoundingMode.HALF_UP).longValueExact();
      return new BitcoinCash(satoshisExact);
   }

   public static BitcoinCash valueOf(long satoshis) {
      return new BitcoinCash(satoshis);
   }

   private static long toLongExact(double origValue) {
      double satoshis = origValue * SATOSHIS_PER_BITCOIN_CASH; // possible loss of
                                                          // precision here
      return Math.round(satoshis);
   }

   private BitcoinCash(long satoshis) {
      if (satoshis < 0)
         throw new IllegalArgumentException(String.format("Bitcoin cash values must be debt-free and positive, but was %s",
               satoshis));
      if (satoshis >= MAX_VALUE)
         throw new IllegalArgumentException(String.format(
               "Bitcoin values must be smaller than 21 Million BTC, but was %s", satoshis));
      this.satoshis = satoshis;
   }

   public BigDecimal multiply(BigDecimal pricePerBch) {
      return toBigDecimal().multiply(BigDecimal.valueOf(satoshis));
   }

   protected BitcoinCash parse(String input) {
      return BitcoinCash.valueOf(input);
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
      return BigDecimal.valueOf(satoshis).divide(SATOSHIS_PER_BITCOIN_CASH_BD);
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

      BitcoinCash bchs = (BitcoinCash) o;

      return satoshis == bchs.satoshis;
   }

   public BigInteger toBigInteger() {
      return BigInteger.valueOf(satoshis);
   }

   public long getLongValue() {
      return satoshis;
   }

   public String toCurrencyString() {
      return BITCOIN_CASH_SYMBOL + ' ' + toString();
   }

   public String toCurrencyString(int decimals) {
      return BITCOIN_CASH_SYMBOL + ' ' + toString(decimals);
   }

   public BitcoinCash roundToSignificantFigures(int n) {
      return BitcoinCash.valueOf(roundToSignificantFigures(satoshis, n));
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
      return (long) (shifted / magnitude);
   }
}