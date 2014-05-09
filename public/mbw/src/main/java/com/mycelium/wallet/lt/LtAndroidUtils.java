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
import com.mycelium.lt.api.model.PublicTraderInfo;
import com.mycelium.wallet.Constants;
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

   public static float calculate5StarRating(PublicTraderInfo info) {
      float traderAgeDays = ((float) info.traderAgeMs) / 1000 / 60 / 60 / 24;

      int successful = info.successfulSales + info.successfulBuys;

      int aborted = info.abortedSales + info.abortedBuys;

      float ageComponent = getAgeRatingComponent(traderAgeDays);
      float successComponent = getVolumeRatingComponent(successful + aborted)
            * getRatingMultiplierBySuccess(successful, aborted);
      float rating = ageComponent + successComponent;

      // Rating should now be a number between -1 and 6

      rating = Math.min(5.0F, rating);
      rating = Math.max(0F, rating);
      return rating;
   }

   /**
    * The number of trades done with a maximum of 4
    */
   private static float getVolumeRatingComponent(int totalTrades) {
      return Math.min((float) totalTrades, 4F);
   }

   private static float getAgeRatingComponent(float traderAgeDays) {
      if (traderAgeDays < 0.1F) {
         // rating is 0 stars if the trader is brand new
         return 0F;
      } else if (traderAgeDays < 0.5) {
         // 0.5 stars if the trader has been around for less than half a day
         return 0.5F;
      } else if (traderAgeDays < 1) {
         // 1 star if the trader has been around for less than 1 day
         return 1F;
      } else if (traderAgeDays < 2) {
         // 1.25 stars if the trader has been around for less than 2 days
         return 1.25F;
      } else if (traderAgeDays < 3) {
         // 1.5 stars if the trader has been around for less than 3 days
         return 1.5F;
      } else if (traderAgeDays < 14) {
         // 1.75 stars if the trader has been around for less than 14 days
         return 1.75F;
      } else {
         // 2 stars if the trader is older than 14 days
         return 2F;
      }
   }

   /**
    * The success multiplier is a number between -1 and 1
    */
   private static float getRatingMultiplierBySuccess(int success, int abort) {
      int total = success + abort;
      if (total == 0) {
         return 0F;
      }
      // Make multiplier a number between 0 and 1 based on success ratio
      float multiplier = ((float) success) / total;

      // make multiplier a number between -1 and 1

      multiplier = (2 * multiplier) - 1;
      return multiplier;
   }

   public static String getApproximateTimeInHours(Context context, long timeInMs) {
      if (timeInMs < Constants.MS_PR_HOUR) {
         return context.getString(R.string.lt_time_less_than_one_hour);
      }
      Long hours = Math.round((double )timeInMs / Constants.MS_PR_HOUR);
      if (hours.equals(1L)) {
         return context.getString(R.string.lt_time_about_one_hour);
      } else {
         return context.getString(R.string.lt_time_about_x_hours, hours);
      }
   }
}
