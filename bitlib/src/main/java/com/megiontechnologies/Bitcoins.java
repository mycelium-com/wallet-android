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
public final class Bitcoins extends BitcoinBase {
   private static final long serialVersionUID = 1L;

   // public static final String BITCOIN_SYMBOL = "\u0243"; // Ƀ
   // public static final String BITCOIN_SYMBOL = "\u0E3F"; // ฿
   public static final String BITCOIN_SYMBOL = "BTC"; // BTC

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

   protected Bitcoins parse(String input) {
      return Bitcoins.valueOf(input);
   }

   @Override
   public boolean equals(Object o) {
      if (this == o)
         return true;
      if (o == null || getClass() != o.getClass())
         return false;

      Bitcoins bitcoins = (Bitcoins) o;

      return satoshis == bitcoins.satoshis;
   }

   @Override
   public String toCurrencyString() {
      return BITCOIN_SYMBOL + ' ' + toString();
   }

   @Override
   public String toCurrencyString(int decimals) {
      return BITCOIN_SYMBOL + ' ' + toString(decimals);
   }
}