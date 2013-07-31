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

package com.mycelium.wallet;

import com.mrd.mbwapi.api.ApiException;
import com.mrd.mbwapi.api.ExchangeSummary;
import com.mrd.mbwapi.api.MyceliumWalletApi;
import com.mrd.mbwapi.api.QueryExchangeSummaryRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public enum CurrencyCode {


   UNKNOWN(0, "UNKNOWN", "?"), USD(1, "USD", "$"), AUD(2, "AUD", "$"), BGN(17, "BGN", "лв") {
      @Override
      public ExchangeSummary[] getRate(MyceliumWalletApi api) throws ApiException {
         return derivedCurrency(api, EUR, 0.51);
      }
   }, CAD(3, "CAD", "$"), CHF(4, "CHF", "CHF"), CNY(5,
           "CNY", "¥"), DKK(6, "DKK", "kr"), EUR(7, "EUR", "€"), GBP(8, "GBP", "£"), HKD(9, "HKD", "$"), JPY(10, "JPY",
           "¥"), NZD(11, "NZD", "$"), PLN(12, "PLN", "zł"), RUB(13, "RUB", "руб"), SEK(14, "SEK", "kr"), SGD(15, "SGD",
           "$"), THB(16, "THB", "฿");

   private static final Map<Integer, CurrencyCode> _intMap = new HashMap<Integer, CurrencyCode>();
   private static final Map<String, CurrencyCode> _stringMap = new HashMap<String, CurrencyCode>();
   private static final CurrencyCode[] SORTED_ARRAY;


   private static ExchangeSummary[] derivedCurrency(MyceliumWalletApi api, CurrencyCode derived, final double rate) throws ApiException {
      ExchangeSummary[] orig = derived.getRate(api);
      ExchangeSummary[] ret = new ExchangeSummary[orig.length];
      for (int i = 0; i < orig.length; i++) {
         ExchangeSummary input = orig[i];
         ret[i] = new ExchangeSummary(input.exchange, input.time, "BGN"
                 , convertCurr(input.high, rate)
                 , convertCurr(input.low, rate)
                 , convertCurr(input.last, rate)
                 , convertCurr(input.bid, rate)
                 , convertCurr(input.ask, rate), 0);
      }
      return ret;
   }

   private static BigDecimal convertCurr(BigDecimal source, double rate) {
      return source.divide(BigDecimal.valueOf(rate), RoundingMode.HALF_EVEN);
   }


   static {
      // Construct map for fast lookup by integer value
      for (CurrencyCode code : CurrencyCode.values()) {
         _intMap.put(code.getCode(), code);
      }

      // Construct map for fast lookup by string value
      for (CurrencyCode code : CurrencyCode.values()) {
         _stringMap.put(code.getShortString(), code);
      }

      // Construct a sorted array not including UNKNOWN
      SORTED_ARRAY = new CurrencyCode[CurrencyCode.values().length - 1];
      int index = 0;
      for (CurrencyCode code : CurrencyCode.values()) {
         if (code == UNKNOWN) {
            continue;
         }
         SORTED_ARRAY[index++] = code;
      }
      // Sort according to currency short string
      Arrays.sort(SORTED_ARRAY, new Comparator<CurrencyCode>() {
         @Override
         public int compare(CurrencyCode c1, CurrencyCode c2) {
            return c1.toString().compareTo(c2.toString());
         }
      });

   }

   /**
    * Get a currency code from an integer
    *
    * @param i the currency code as an integer
    * @return The corresponding CurrencyCode or UNKNOWN if no match was found
    */
   public static CurrencyCode fromInt(int i) {
      CurrencyCode code = _intMap.get(i);
      if (code == null) {
         code = UNKNOWN;
      }
      return code;
   }

   /**
    * Get a currency code from its short string
    *
    * @param shortString three-letter code
    * @return The corresponding CurrencyCode or UNKNOWN if no match was found
    */
   public static CurrencyCode fromShortString(String shortString) {
      CurrencyCode code = _stringMap.get(shortString);
      if (code == null) {
         code = UNKNOWN;
      }
      return code;
   }

   public static CurrencyCode[] sortedArray() {
      return SORTED_ARRAY;
   }

   @Override
   public String toString() {
      return _shortString;
   }

   private int _code;
   private String _shortString;
   private String _symbol;

   private CurrencyCode(int code, String shortString, String symbol) {
      _code = code;
      _shortString = shortString;
      _symbol = symbol;
   }

   private CurrencyCode() {
      // Used by Jackson
   }

   public int getCode() {
      return _code;
   }

   public String getShortString() {
      return _shortString;
   }

   public String getSymbol() {
      return _symbol;
   }

   public ExchangeSummary[] getRate(MyceliumWalletApi api) throws ApiException {
      return api.queryExchangeSummary(new QueryExchangeSummaryRequest(_shortString)).exchangeSummaries;
   }
}
