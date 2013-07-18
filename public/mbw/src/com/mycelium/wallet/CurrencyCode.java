package com.mycelium.wallet;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public enum CurrencyCode {

   UNKNOWN(0, "UNKNOWN", "?"), USD(1, "USD", "$"), AUD(2, "AUD", "$"), CAD(3, "CAD", "$"), CHF(4, "CHF", "CHF"), CNY(5,
         "CNY", "¥"), DKK(6, "DKK", "kr"), EUR(7, "EUR", "€"), GBP(8, "GBP", "£"), HKD(9, "HKD", "$"), JPY(10, "JPY",
         "¥"), NZD(11, "NZD", "$"), PLN(12, "PLN", "zł"), RUB(13, "RUB", "руб"), SEK(14, "SEK", "kr"), SGD(15, "SGD",
         "$"), THB(16, "THB", "฿");

   private static final Map<Integer, CurrencyCode> _intMap = new HashMap<Integer, CurrencyCode>();
   private static final Map<String, CurrencyCode> _stringMap = new HashMap<String, CurrencyCode>();
   private static final CurrencyCode[] SORTED_ARRAY;

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
    * @param i
    *           the currency code as an integer
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

}
