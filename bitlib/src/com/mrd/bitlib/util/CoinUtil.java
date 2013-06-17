/*
 * Copyright 2013 Megion Research and Development GmbH
 *
 *  Licensed under the Microsoft Reference Source License (MS-RSL)
 *
 *  This license governs use of the accompanying software. If you use the software, you accept this license.
 *  If you do not accept the license, do not use the software.
 *
 *  1. Definitions
 *  The terms "reproduce," "reproduction," and "distribution" have the same meaning here as under U.S. copyright law.
 *  "You" means the licensee of the software.
 *  "Your company" means the company you worked for when you downloaded the software.
 *  "Reference use" means use of the software within your company as a reference, in read only form, for the sole purposes
 *  of debugging your products, maintaining your products, or enhancing the interoperability of your products with the
 *  software, and specifically excludes the right to distribute the software outside of your company.
 *  "Licensed patents" means any Licensor patent claims which read directly on the software as distributed by the Licensor
 *  under this license.
 *
 *  2. Grant of Rights
 *  (A) Copyright Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 *  worldwide, royalty-free copyright license to reproduce the software for reference use.
 *  (B) Patent Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 *  worldwide, royalty-free patent license under licensed patents for reference use.
 *
 *  3. Limitations
 *  (A) No Trademark License- This license does not grant you any rights to use the Licensor’s name, logo, or trademarks.
 *  (B) If you begin patent litigation against the Licensor over patents that you think may apply to the software
 *  (including a cross-claim or counterclaim in a lawsuit), your license to the software ends automatically.
 *  (C) The software is licensed "as-is." You bear the risk of using it. The Licensor gives no express warranties,
 *  guarantees or conditions. You may have additional consumer rights under your local laws which this license cannot
 *  change. To the extent permitted under your local laws, the Licensor excludes the implied warranties of merchantability,
 *  fitness for a particular purpose and non-infringement.
 *
 */

package com.mrd.bitlib.util;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Utility for turning an amount of satoshis into a user friendly string. 1
 * satoshi == 0.000000001 BTC
 */
public class CoinUtil {

   private static final BigDecimal BTC_IN_SATOSHIS = new BigDecimal(100000000);
   private static final BigDecimal mBTC_IN_SATOSHIS = new BigDecimal(100000);
   private static final BigDecimal uBTC_IN_SATOSHIS = new BigDecimal(100);

   public enum Denomination {
      BTC(8, "BTC", "BTC", BTC_IN_SATOSHIS), mBTC(5, "mBTC", "mBTC", mBTC_IN_SATOSHIS), uBTC(2, "uBTC",
            "\u00B5BTC", uBTC_IN_SATOSHIS);

      private final int _decimalPlaces;
      private final String _asciiString;
      private final String _unicodeString;
      private final BigDecimal _oneUnitInSatoshis;

      private Denomination(int decimalPlaces, String asciiString, String unicodeString, BigDecimal oneUnitInSatoshis) {
         _decimalPlaces = decimalPlaces;
         _asciiString = asciiString;
         _unicodeString = unicodeString;
         _oneUnitInSatoshis = oneUnitInSatoshis;
      }

      public int getDecimalPlaces() {
         return _decimalPlaces;
      }

      public String getAsciiName() {
         return _asciiString;
      }

      public String getUnicodeName() {
         return _unicodeString;
      }

      public BigDecimal getOneUnitInSatoshis() {
         return _oneUnitInSatoshis;
      }

      @Override
      public String toString() {
         return _asciiString;
      }

      public static Denomination fromString(String string) {
         if (string.equals("BTC")) {
            return BTC;
         } else if (string.equals("mBTC")) {
            return mBTC;
         } else if (string.equals("uBTC")) {
            return uBTC;
         } else {
            return BTC;
         }
      }

   };

   /**
    * Number of satoshis in a Bitcoin.
    */
   public static final BigInteger BTC = new BigInteger("100000000", 10);

   /**
    * Get the given value in satoshis as a string on the form "10.12345"
    * denominated in BTC.
    * <p>
    * This method only returns necessary decimal points to tell the exact value.
    * If you wish to display all digits use
    * {@link CoinUtil#fullValueString(long)}
    * 
    * @param value
    *           The number of satoshis
    * @return The given value in satoshis as a string on the form "10.12345".
    */
   public static String valueString(long value) {
      return valueString(value, Denomination.BTC);
   }

   /**
    * Get the given value in satoshis as a string on the form "10.12345" using
    * the specified denomination.
    * <p>
    * This method only returns necessary decimal points to tell the exact value.
    * If you wish to display all digits use
    * {@link CoinUtil#fullValueString(long, Denomination)}
    * 
    * @param value
    *           The number of satoshis
    * @param denomination
    *           The denomination to use
    * @return The given value in satoshis as a string on the form "10.12345".
    */
   public static String valueString(long value, Denomination denomination) {
      BigDecimal d = BigDecimal.valueOf(value);
      d = d.divide(denomination.getOneUnitInSatoshis());
      return d.toPlainString();
   }

   /**
    * Get the given value in satoshis as a string on the form "10.12345000"
    * denominated in BTC.
    * <p>
    * This method always returns a string with 8 decimal points. If you only
    * wish to have the necessary digits use {@link CoinUtil#valueString(long)}
    * 
    * @param value
    *           The number of satoshis
    * @return The given value in satoshis as a string on the form "10.12345000".
    */
   public static String fullValueString(long value) {
      return fullValueString(value, Denomination.BTC);
   }

   /**
    * Get the given value in satoshis as a string on the form "10.12345000"
    * using the specified denomination.
    * <p>
    * This method always returns a string with all decimal points. If you only
    * wish to have the necessary digits use
    * {@link CoinUtil#valueString(long, Denomination)}
    * 
    * @param value
    *           The number of satoshis
    * @param denomination
    *           The denomination to use
    * @return The given value in satoshis as a string on the form "10.12345000".
    */
   public static String fullValueString(long value, Denomination denomination) {
      BigDecimal d = BigDecimal.valueOf(value);
      d = d.movePointLeft(denomination.getDecimalPlaces());
      return d.toPlainString();
   }

}
