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


import com.google.common.base.Optional;
import com.megiontechnologies.Bitcoins;

import java.io.Serializable;
import java.math.BigDecimal;

public abstract class CurrencyValue implements Serializable {
   protected static final String BTC = "BTC";

   public abstract String getCurrency();

   public abstract BigDecimal getValue();

   public boolean isZero(){
      return BigDecimal.ZERO.compareTo(getValue()) == 0;
   }
   
   @Override
   public String toString() {
      return getValue() + " " + getCurrency();
   }

   static public CurrencyValue fromValue(CurrencyValue currencyValue, String targetCurrency, ExchangeRateProvider exchangeRateManager) {
      if (currencyValue instanceof ExchangeBasedCurrencyValue) {
         return fromValue((ExchangeBasedCurrencyValue) currencyValue, targetCurrency, exchangeRateManager);
      } else if (currencyValue instanceof ExactCurrencyValue) {
         return fromValue((ExactCurrencyValue) currencyValue, targetCurrency, exchangeRateManager);
      } else {
         throw new RuntimeException("Unable to convert from currency value " + currencyValue.getClass().toString());
      }
   }

   public static CurrencyValue fromValue(ExchangeBasedCurrencyValue value, String targetCurrency, ExchangeRateProvider exchangeRateManager) {
      if (value.getCurrency().equals(targetCurrency)) {
         return value;
      } else {
         return ExchangeBasedCurrencyValue.fromValue(value, targetCurrency, exchangeRateManager);
      }
   }

   public static CurrencyValue fromValue(ExactCurrencyValue value, String targetCurrency, ExchangeRateProvider exchangeRateManager) {
      if (value.getCurrency().equals(targetCurrency)) {
         return value;
      } else {
         return ExchangeBasedCurrencyValue.fromValue(value, targetCurrency, exchangeRateManager);
      }
   }

   public boolean isBtc() {
      return getCurrency().equals(BTC);
   }

   public boolean isFiat() {
      return !isBtc();
   }

   public BitcoinValue getBitcoinValue(ExchangeRateProvider exchangeRateManager) {
      if (this instanceof BitcoinValue) {
         return ((BitcoinValue) this);
      } else {
         if (this instanceof ExactCurrencyValue) {
            return ExchangeBasedBitcoinValue.fromValue((ExactCurrencyValue) this, exchangeRateManager);
         } else if (this instanceof ExchangeBasedCurrencyValue) {
            return ExchangeBasedBitcoinValue.fromValue(this.getExactValue(), exchangeRateManager);
         } else {
            throw new RuntimeException("Unable to convert to Bitcoin");
         }
      }
   }

   public Bitcoins getAsBitcoin(ExchangeRateProvider exchangeRateManager) {
      return getBitcoinValue(exchangeRateManager).getAsBitcoin();
   }

   public ExactCurrencyValue getExactValue() {
      throw new RuntimeException("Unable to provide exact value");
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }

      CurrencyValue that = (CurrencyValue) o;

      if (!getCurrency().equals(that.getCurrency())) {
         return false;
      }
      return getValue().compareTo(that.getValue()) == 0;
   }

   @Override
   public int hashCode() {
      int result = getCurrency().hashCode();
      result = 31 * result + getValue().hashCode();
      return result;
   }

   public static Optional<ExactFiatValue> checkUsdAmount(CurrencyValue amount) {
      boolean isUSD = amount != null
              && (amount instanceof ExactFiatValue)
              && amount.getCurrency().equals("USD");
      if (isUSD) {
         return Optional.of((ExactFiatValue) amount);
      } else {
         return Optional.absent();
      }
   }
}
