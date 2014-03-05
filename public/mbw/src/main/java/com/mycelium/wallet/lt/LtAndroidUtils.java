package com.mycelium.wallet.lt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.mycelium.lt.api.model.PriceFormula;

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
      List<PremiumChoice> choices = new ArrayList<PremiumChoice>();
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
      ArrayAdapter<PremiumChoice> dataAdapter = new ArrayAdapter<PremiumChoice>(context,
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

   public static void populatePriceFormulaSpinner(Context context, Spinner spinner, List<PriceFormula> priceFormulas,
         PriceFormula toSelect) {
      // Build list of choices and find index to select
      List<PriceFormulaChoice> choices = new LinkedList<PriceFormulaChoice>();
      int indexToSelect = -1;
      for (int i = 0; i < priceFormulas.size(); i++) {
         PriceFormula formula = priceFormulas.get(i);
         if (formula.equals(toSelect)) {
            indexToSelect = i;
         }
         choices.add(new PriceFormulaChoice(formula));
      }

      // If not found add at the end
      if (indexToSelect == -1) {
         if (toSelect == null) {
            indexToSelect = 0;
         } else {
            indexToSelect = choices.size();
            choices.add(new PriceFormulaChoice(toSelect));
         }
      }

      // Populate and select
      ArrayAdapter<PriceFormulaChoice> dataAdapter = new ArrayAdapter<PriceFormulaChoice>(context,
            android.R.layout.simple_spinner_item, choices);
      dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
      spinner.setAdapter(dataAdapter);
      if (choices.size() > 0) {
         spinner.setSelection(indexToSelect);
      }
   }

}
