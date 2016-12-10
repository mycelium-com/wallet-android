/*
 * Copyright 2013, 2014 Megion Research and Development GmbH
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

package com.mycelium.wallet.lt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.mycelium.lt.api.model.PriceFormula;
import com.mycelium.wallet.Constants;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;

public class LtAndroidUtils {

   public static class PremiumChoice {
      public final double premium;
      public final String name;

      private PremiumChoice(double premium) {
         this.premium = premium;
         // Make the name something like "-6%"
         char sign;
         if (premium > 0) {
            sign = '+';
         } else if (premium < 0) {
            sign = '-';
         } else {
            sign = ' ';
         }
         double d = Math.abs(premium);
         Locale locale = new Locale("en", "US");
         String premiumString = d == (int) d ? String.format(locale, "%d", (int) d) : String.format(locale, "%s", d);
         name = String.format(locale, "%c%s%%", sign, premiumString);
      }

      @Override
      public String toString() {
         return name;
      }

   }

   public static void populatePremiumSpinner(Context context, Spinner spinner, double premiumToSelect) {

      // Make list of choices and see if the premium we already have is in the
      // list
      List<PremiumChoice> choices = new ArrayList<>();
      boolean foundSelection = false;
      for (double premium : LtAndroidConstants.PREMIUM_CHOICES) {
         if (premium == premiumToSelect) {
            foundSelection = true;
         }
         choices.add(new PremiumChoice(premium));
      }

      // If not found, add it and sort
      if (!foundSelection) {
         choices.add(new PremiumChoice(premiumToSelect));
         // Sort
         Collections.sort(choices, new Comparator<PremiumChoice>() {

            @Override
            public int compare(PremiumChoice lhs, PremiumChoice rhs) {
               if (lhs.premium < rhs.premium) {
                  return -1;
               } else if (lhs.premium > rhs.premium) {
                  return 1;
               } else {
                  return 0;
               }
            }
         });
      }

      // Find index to select
      int indexToSelect = 0;
      for (int i = 0; i < choices.size(); i++) {
         if (premiumToSelect == choices.get(i).premium) {
            indexToSelect = i;
         }
      }

      // Populate and select
      ArrayAdapter<PremiumChoice> dataAdapter = new ArrayAdapter<>(context,
              android.R.layout.simple_spinner_item, choices);
      dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
      spinner.setAdapter(dataAdapter);
      spinner.setSelection(indexToSelect);
   }

   public static class PriceFormulaChoice {
      public final PriceFormula formula;

      private PriceFormulaChoice(PriceFormula formula) {
         this.formula = formula;
      }

      @Override
      public String toString() {
         return formula.name;
      }
   }

   /**
    * populates the price formula spinner (currently list of exchanges and indices)
    *
    * @param activity      the Activity
    * @param spinner       the Spinner
    * @param priceFormulas the price formulas (data to back the spinner)
    * @param toSelect      the price formula that is to be added if missing and selected. If null, the accounts current
    *                      price formula is used.
    */
   public static void populatePriceFormulaSpinner(Activity activity, Spinner spinner, List<PriceFormula> priceFormulas,
                                                  PriceFormula toSelect) {
      // Build list of choices and find index to select
      List<PriceFormulaChoice> choices = new LinkedList<>();
      int indexToSelect = -1;
      for (int i = 0; i < priceFormulas.size(); i++) {
         PriceFormula formula = priceFormulas.get(i);
         if (formula.equals(toSelect)) {
            indexToSelect = i;
         }
         choices.add(new PriceFormulaChoice(formula));
      }
      if (indexToSelect == -1) {
         // not found
         if (toSelect == null) {
            // and no preference: default to account default or first
            indexToSelect = 0;
            MbwManager mbwManager = MbwManager.getInstance(activity.getApplication());
            String currentRateName = mbwManager.getExchangeRateManager().getCurrentExchangeSourceName();
            if(currentRateName != null) {
               for (int i = 0; i < priceFormulas.size(); i++) {
                  PriceFormula formula = priceFormulas.get(i);
                  if (currentRateName.equals(formula.name)) {
                     indexToSelect = i;
                     break;
                  }
               }
            }
         } else {
            // add at default at the end if not null
            indexToSelect = choices.size();
            choices.add(new PriceFormulaChoice(toSelect));
         }
      }
      // Populate and select
      ArrayAdapter<PriceFormulaChoice> dataAdapter = new ArrayAdapter<>(activity,
              android.R.layout.simple_spinner_item, choices);
      dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
      spinner.setAdapter(dataAdapter);
      if (choices.size() > 0) {
         spinner.setSelection(indexToSelect);
      }
   }

   public static String getApproximateTimeInHours(Context context, long timeInMs) {
      if (timeInMs < Constants.MS_PR_HOUR) {
         return context.getString(R.string.lt_time_less_than_one_hour);
      }
      Long hours = Math.round((double) timeInMs / Constants.MS_PR_HOUR);
      if (hours.equals(1L)) {
         return context.getString(R.string.lt_time_about_one_hour);
      } else {
         return context.getString(R.string.lt_time_about_x_hours, Long.toString(hours));
      }
   }

   public static String getTimeSpanString(Context context, long ms) {
      // Less than one minute
      if (ms < Constants.MS_PR_MINUTE) {
         return context.getString(R.string.lt_time_less_than_one_minute);
      }
      // Less than one hour
      if (ms < Constants.MS_PR_HOUR) {
         long minutes = ms / Constants.MS_PR_MINUTE;
         if (minutes == 1) {
            return context.getString(R.string.lt_time_one_minute);
         } else {
            return context.getString(R.string.lt_time_in_minutes, Long.toString(minutes));
         }
      }
      // Less than one day
      if (ms < Constants.MS_PR_DAY) {
         long hours = ms / Constants.MS_PR_HOUR;
         if (hours == 1) {
            return context.getString(R.string.lt_time_one_hour);
         } else {
            return context.getString(R.string.lt_time_in_hours, Long.toString(hours));
         }
      }

      // One day or more
      long days = ms / Constants.MS_PR_DAY;
      if (days == 1) {
         return context.getString(R.string.lt_time_one_day);
      } else {
         return context.getString(R.string.lt_time_in_days, Long.toString(days));
      }
   }
}
