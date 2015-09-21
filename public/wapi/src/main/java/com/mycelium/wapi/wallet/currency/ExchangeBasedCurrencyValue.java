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

package com.mycelium.wapi.wallet.currency;

import com.mycelium.wapi.model.ExchangeRate;

import java.math.BigDecimal;
import java.math.RoundingMode;

public abstract class ExchangeBasedCurrencyValue extends CurrencyValue {
   private final String currency;
   private final ExactCurrencyValue basedOnExactValue;
   private final ExchangeRate usedSourceExchangeRate;
   private final ExchangeRate usedTargetExchangeRate;

   static public CurrencyValue fromValue(CurrencyValue currencyValue, String targetCurrency, ExchangeRateProvider exchangeRateManager) {
      if (currencyValue instanceof ExchangeBasedCurrencyValue) {
         return fromValue((ExchangeBasedCurrencyValue) currencyValue, targetCurrency, exchangeRateManager);
      } else if (currencyValue instanceof ExactCurrencyValue) {
         return fromValue((ExactCurrencyValue) currencyValue, targetCurrency, exchangeRateManager);
      } else {
         throw new RuntimeException("Unable to convert from currency value " + currencyValue.getClass().toString());
      }
   }

   static public CurrencyValue fromValue(ExchangeBasedCurrencyValue currencyValue, String targetCurrency, ExchangeRateProvider exchangeRateManager) {
      return fromExactValue(currencyValue.getExactValue(), targetCurrency, exchangeRateManager);
   }

   static public CurrencyValue fromValue(ExactCurrencyValue currencyValue, String targetCurrency, ExchangeRateProvider exchangeRateManager) {
      return fromExactValue(currencyValue, targetCurrency, exchangeRateManager);
   }

   static public CurrencyValue fromExactValue(ExactCurrencyValue exactValue, String targetCurrency, ExchangeRateProvider exchangeRateManager) {
      if (targetCurrency.equals(exactValue.getCurrency())) {
         return exactValue;
      } else {
         String sourceCurrency = exactValue.getCurrency();
         BigDecimal sourcePrice = null;
         BigDecimal targetPrice = null;
         ExchangeRate sourceExchangeRate = null;
         ExchangeRate targetExchangeRate = null;

         if (!sourceCurrency.equals(BTC)) {
            sourceExchangeRate = exchangeRateManager.getExchangeRate(sourceCurrency);
            if (sourceExchangeRate != null && sourceExchangeRate.price != null) {
               sourcePrice = BigDecimal.valueOf(sourceExchangeRate.price);
            }
         } else {
            sourcePrice = BigDecimal.ONE;
         }

         if (!targetCurrency.equals(BTC)) {
            targetExchangeRate = exchangeRateManager.getExchangeRate(targetCurrency);
            if (targetExchangeRate != null && targetExchangeRate.price != null) {
               targetPrice = BigDecimal.valueOf(targetExchangeRate.price);
            }
         } else {
            targetPrice = BigDecimal.ONE;
         }

         if (sourcePrice == null || targetPrice == null) {
            if (targetCurrency.equals(CurrencyValue.BTC)) {
               return new ExchangeBasedBitcoinValue(targetCurrency, (Long) null, exactValue, sourceExchangeRate, targetExchangeRate);
            } else {
               return new ExchangeBasedFiatValue(targetCurrency, null, exactValue, sourceExchangeRate, targetExchangeRate);
            }
         }

         BigDecimal exactDecimal = exactValue.getValue();
         BigDecimal newValue = null;
         if (exactDecimal != null) {
            newValue = exactDecimal.divide(sourcePrice, 8, RoundingMode.HALF_UP).multiply(targetPrice);
         }
         if (targetCurrency.equals(CurrencyValue.BTC)) {
            return new ExchangeBasedBitcoinValue(targetCurrency, newValue, exactValue, sourceExchangeRate, targetExchangeRate);
         } else {
            return new ExchangeBasedFiatValue(targetCurrency, newValue, exactValue, sourceExchangeRate, targetExchangeRate);
         }
      }
   }

   protected ExchangeBasedCurrencyValue(String currency, ExactCurrencyValue basedOnExactValue, ExchangeRate usedSourceExchangeRate, ExchangeRate usedTargetExchangeRate) {
      this.currency = currency;
      this.basedOnExactValue = basedOnExactValue;
      this.usedSourceExchangeRate = usedSourceExchangeRate;
      this.usedTargetExchangeRate = usedTargetExchangeRate;
   }

   @Override
   public String getCurrency() {
      return currency;
   }


   @Override
   public ExactCurrencyValue getExactValue() {
      return basedOnExactValue;
   }

}

