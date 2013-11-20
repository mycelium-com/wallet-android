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

import android.annotation.SuppressLint;

public enum ExchangeRateCalculationMode {

   UNKNOWN(0, "UNKNOWN", "UNKNOWN"), MT_GOX(1, "MtGox", "MtGox"), BITSTAMP(2, "Bitstamp", "Bitstamp"), WEIGHTED_AVERAGE(
         3, "Weighted Average", "Avg");

   @SuppressLint("UseSparseArrays")
   private static final ExchangeRateCalculationMode[] ORDERED_ARRAY;

   static {

      // Construct an ordered array not including UNKNOWN
      ORDERED_ARRAY = new ExchangeRateCalculationMode[ExchangeRateCalculationMode.values().length - 1];
      ORDERED_ARRAY[0] = BITSTAMP;
      ORDERED_ARRAY[1] = MT_GOX;
      ORDERED_ARRAY[2] = WEIGHTED_AVERAGE;
   }

   /**
    * Get a calculation method from an integer
    */
   public static ExchangeRateCalculationMode fromInt(int i) {
      switch (i) {
      case 0:
         return UNKNOWN;
      case 1:
         return MT_GOX;
      case 2:
         return BITSTAMP;
      case 3:
         return WEIGHTED_AVERAGE;
      default:
         return UNKNOWN;
      }
   }

   /**
    * Get a calculation method from its name
    */
   public static ExchangeRateCalculationMode fromName(String name) {
      if (name == null) {
         return UNKNOWN;
      }
      if (name.equals(UNKNOWN.toString())) {
         return UNKNOWN;
      } else if (name.equals(MT_GOX.toString())) {
         return MT_GOX;
      } else if (name.equals(BITSTAMP.toString())) {
         return BITSTAMP;
      } else if (name.equals(WEIGHTED_AVERAGE.toString())) {
         return WEIGHTED_AVERAGE;
      } else {
         return UNKNOWN;
      }
   }

   public static ExchangeRateCalculationMode[] orderedArray() {
      return ORDERED_ARRAY;
   }

   public static String[] orderedNames() {
      String[] names = new String[ORDERED_ARRAY.length];
      for (int i = 0; i < ORDERED_ARRAY.length; i++) {
         names[i] = ORDERED_ARRAY[i].toString();
      }
      return names;
   }

   private int _code;
   private String _name;
   private String _shortName;

   private ExchangeRateCalculationMode(int code, String name, String shortName) {
      _code = code;
      _name = name;
      _shortName = shortName;
   }

   public int getCode() {
      return _code;
   }

   @Override
   public String toString() {
      return _name;
   }

   public String getShortName() {
      return _shortName;
   }

}
