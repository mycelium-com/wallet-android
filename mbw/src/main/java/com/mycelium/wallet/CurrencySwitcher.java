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

package com.mycelium.wallet;

import com.google.api.client.util.Lists;
import com.mrd.bitlib.util.CoinUtil;
import com.mycelium.wallet.exchange.ExchangeRateManager;
import com.mycelium.wallet.exchange.ValueSum;
import com.mycelium.wapi.model.ExchangeRate;
import com.mycelium.wapi.wallet.bch.coins.BchCoin;
import com.mycelium.wapi.wallet.bch.coins.BchMain;
import com.mycelium.wapi.wallet.bch.coins.BchTest;
import com.mycelium.wapi.wallet.btc.coins.BitcoinMain;
import com.mycelium.wapi.wallet.btc.coins.BitcoinTest;
import com.mycelium.wapi.wallet.coins.GenericAssetInfo;
import com.mycelium.wapi.wallet.coins.Value;
import com.mycelium.wapi.wallet.currency.CurrencyValue;
import com.mycelium.wapi.wallet.fiat.coins.FiatType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CurrencySwitcher {
   private final ExchangeRateManager exchangeRateManager;

   private List<GenericAssetInfo> fiatCurrencies;
   private CoinUtil.Denomination bitcoinDenomination;

  private CoinUtil.Denomination bitcoinCashDenomination;

   // the last selected/shown fiat currency
   private GenericAssetInfo currentFiatCurrency;

   // the last shown currency (usually same as fiat currency, but in some spots we cycle through all currencies including Bitcoin)
    private GenericAssetInfo currentCurrency;
    private GenericAssetInfo defaultCurrency = BuildConfig.FLAVOR.equals("prodnet") ? BitcoinMain.get() : BitcoinTest.get();

   public CurrencySwitcher(final ExchangeRateManager exchangeRateManager, final Set<GenericAssetInfo> fiatCurrencies
           , GenericAssetInfo currentCurrency, final CoinUtil.Denomination bitcoinDenomination) {
      this.exchangeRateManager = exchangeRateManager;
      ArrayList<GenericAssetInfo> currencies = Lists.newArrayList(fiatCurrencies);
      Collections.sort(currencies, new Comparator<GenericAssetInfo>() {
          @Override
          public int compare(GenericAssetInfo cryptoCurrency, GenericAssetInfo t1) {
              return cryptoCurrency.getSymbol().compareTo(t1.getSymbol());
          }
      });
      this.fiatCurrencies = currencies;
      this.bitcoinDenomination = bitcoinDenomination;
      this.bitcoinCashDenomination = CoinUtil.Denomination.BCH;

      this.currentCurrency = currentCurrency;

      // if BTC is selected or current currency is not in list of available currencies (e.g. after update)
      // select a default one or none
      if (currentCurrency.equals(BitcoinMain.get()) || currentCurrency.equals(BitcoinTest.get())
              || currentCurrency.equals(BchMain.INSTANCE) || currentCurrency.equals(BchTest.INSTANCE)
              || !fiatCurrencies.contains(currentCurrency)) {
         if (fiatCurrencies.size() == 0) {
            this.currentFiatCurrency = null;  // no fiat currency selected
         } else {
            this.currentFiatCurrency = currencies.get(0);
         }
      } else {
         this.currentFiatCurrency = currentCurrency;
      }
   }

   public ExchangeRateManager getExchangeRateManager() {
      return exchangeRateManager;
   }

   public void setCurrency(GenericAssetInfo setToCurrency) {
      //TODO need no accurate detect is colu currency
      if (isFiatCurrency(setToCurrency)) {
         currentFiatCurrency = setToCurrency;
      }
      currentCurrency = setToCurrency;
   }

   public boolean isFiatCurrency(GenericAssetInfo currency) {
      return currency instanceof FiatType;
   }

   public GenericAssetInfo getDefaultCurrency() {
      return defaultCurrency;
   }

   public GenericAssetInfo getCurrentFiatCurrency() {
      return currentFiatCurrency;
   }

   public GenericAssetInfo getCurrentCurrency() {
      return currentCurrency;
   }

   public String getCurrentCurrencyIncludingDenomination() {
      if (currentCurrency.equals(BitcoinMain.get()) || currentCurrency.equals(BitcoinTest.get())) {
          return bitcoinDenomination.getUnicodeName();
      }else if(currentCurrency instanceof BchCoin) {
              return bitcoinDenomination.getUnicodeName().replace(CurrencyValue.BTC, CurrencyValue.BCH);
      }else {
          return currentCurrency.getSymbol();
      }
   }

   public List<GenericAssetInfo> getCurrencyList(GenericAssetInfo ... additions) {
      //make a copy to prevent others from changing our internal list
      List<GenericAssetInfo> result = new ArrayList<>(fiatCurrencies);
      Collections.addAll(result, additions);
      return result;
   }

   public void setCurrencyList(final Set<GenericAssetInfo> fiatCurrencies) {
      // convert the set to a list and sort it
      ArrayList<GenericAssetInfo> currencies = Lists.newArrayList(fiatCurrencies);
      Collections.sort(currencies, new Comparator<GenericAssetInfo>() {
          @Override
          public int compare(GenericAssetInfo abstractAsset, GenericAssetInfo t1) {
              return abstractAsset.getSymbol().compareTo(t1.getSymbol());
          }
      });

      //if we de-selected our current active currency, we switch it
      if (!currencies.contains(currentFiatCurrency)) {
         if (currencies.isEmpty()) {
            //no fiat
            setCurrency(null);
         } else {
            setCurrency(currencies.get(0));
         }
      }
      //copy to prevent changes by caller
      this.fiatCurrencies = new ArrayList<>(currencies);
   }

   public void setDefaultCurrency(GenericAssetInfo currency) {
//      Set<GenericAssetInfo> currencies = new HashSet<>(getCurrencyList());
//      if (!defaultCurrency.equals(currency.getSymbol())) {
//         currencies.remove(defaultCurrency);
//         currencies.add(currency);
//      }
      defaultCurrency = currency;
   }

   public GenericAssetInfo getNextCurrency(boolean includeBitcoin) {
      List<GenericAssetInfo> currencies = getCurrencyList();

      //just to be sure we dont cycle through a single one
      if (!includeBitcoin && currencies.size() <= 1) {
         return currentFiatCurrency;
      }

      int index = currencies.indexOf(currentCurrency);
      index++; //hop one forward

      if (index >= currencies.size()) {
         // we are at the end of the fiat-list. return BTC if we should include Bitcoin, otherwise wrap around
         if (includeBitcoin) {
            // only set currentCurrency, but leave currentFiat currency as it was
            currentCurrency = defaultCurrency;
         } else {
            index -= currencies.size(); //wrap around
            currentCurrency = currencies.get(index);
            currentFiatCurrency = currentCurrency;
         }
      } else {
         currentCurrency = currencies.get(index);
         currentFiatCurrency = currentCurrency;
      }

      exchangeRateManager.requestOptionalRefresh();

      return currentCurrency;
   }

   public CoinUtil.Denomination getBitcoinDenomination() {
      return bitcoinDenomination;
   }

  public CoinUtil.Denomination getBitcoinCashDenomination() {
    return bitcoinCashDenomination;
  }

   public void setBitcoinDenomination(CoinUtil.Denomination _bitcoinDenomination) {
      this.bitcoinDenomination = _bitcoinDenomination;
   }

  public void setBitcoinCashDenomination(CoinUtil.Denomination _bitcoinCashDenomination) {
    this.bitcoinCashDenomination = _bitcoinCashDenomination;
  }

   public String getBtcValueString(long satoshis) {
      return getBtcValueString(satoshis, true);
   }

  public String getBchValueString(long satoshis) {
    return getBchValueString(satoshis, true);
  }

   public String getBtcValueString(long satoshis, boolean includeUnit) {
      CoinUtil.Denomination d = getBitcoinDenomination();
      String valueString = CoinUtil.valueString(satoshis, d, true);
      if (includeUnit) {
         return valueString + " " + d.getUnicodeName();
      } else {
         return valueString;
      }
   }

  public String getBchValueString(long satoshis, boolean includeUnit) {
    CoinUtil.Denomination d = getBitcoinCashDenomination();
    String valueString = CoinUtil.valueString(satoshis, d, true);
    if (includeUnit) {
      return valueString + " " + d.getUnicodeName();
    } else {
      return valueString;
    }
  }

   public String getBtcValueString(long satoshis, boolean includeUnit, int precision) {
      CoinUtil.Denomination d = getBitcoinDenomination();
      String valueString = CoinUtil.valueString(satoshis, d, precision);
      if (includeUnit) {
         return valueString + " " + d.getUnicodeName();
      } else {
         return valueString;
      }
   }


   public boolean isFiatExchangeRateAvailable() {
      if (currentFiatCurrency == null) {
         // we dont even have a fiat currency...
         return false;
      }

      // check if there is a rate available
      ExchangeRate rate = exchangeRateManager.getExchangeRate(getCurrentFiatCurrency());
      return rate != null && rate.price != null;
   }

   public String getFormattedFiatValue(CurrencyValue value, boolean includeCurrencyCode) {
      if (value == null){
         return "";
      }

      CurrencyValue targetCurrency = getAsFiatValue(value);

      if (currentFiatCurrency == null) {
         return "";
      }

      if (targetCurrency == null) {
         //todo
         return "";
      } else {
         if (includeCurrencyCode) {
            return Utils.getFormattedValueWithUnit(targetCurrency, getBitcoinDenomination());
         } else {
            return Utils.getFormattedValue(targetCurrency, getBitcoinDenomination());
         }
      }
   }

   public String getFormattedFiatValue(CurrencyValue value, boolean includeCurrencyCode, int precision) {
      if (currentFiatCurrency == null) {
         return "";
      }

      CurrencyValue targetCurrency = getAsFiatValue(value);

      if (targetCurrency == null) {
         return "";
      } else {
         if (includeCurrencyCode) {
            return Utils.getFormattedValueWithUnit(targetCurrency, getBitcoinDenomination(), precision);
         } else {
            return Utils.getFormattedValue(targetCurrency, getBitcoinDenomination(), precision);
         }
      }
   }

   public String getFormattedValue(CurrencyValue currencyValue, boolean includeCurrencyCode) {
      if (currencyValue == null){
         return "";
      }
      CurrencyValue targetCurrency = getAsValue(currencyValue);
      if (includeCurrencyCode) {
         return Utils.getFormattedValueWithUnit(targetCurrency, getBitcoinDenomination());
      } else {
         return Utils.getFormattedValue(targetCurrency, getBitcoinDenomination());
      }
   }

   public String getFormattedValue(CurrencyValue currencyValue, boolean includeCurrencyCode, int precision) {
      if (currencyValue == null){
         return "";
      }
      CurrencyValue targetCurrency = getAsValue(currencyValue);
      if (includeCurrencyCode) {
         return Utils.getFormattedValueWithUnit(targetCurrency, getBitcoinDenomination(), precision);
      } else {
         return Utils.getFormattedValue(targetCurrency, getBitcoinDenomination(), precision);
      }
   }

//    public String getFormattedValue(Value currencyValue, boolean includeCurrencyCode, int precision) {
//        if (currencyValue == null){
//            return "";
//        }
//        CurrencyValue targetCurrency = getAsValue(currencyValue);
//        if (includeCurrencyCode) {
//            return Utils.getFormattedValueWithUnit(targetCurrency, getBitcoinDenomination(), precision);
//        } else {
//            return Utils.getFormattedValue(targetCurrency, getBitcoinDenomination(), precision);
//        }
//    }


    public Value getAsFiatValue(Value value) {
        if (value == null) {
            return null;
        }
        if (currentFiatCurrency == null) {
            return null;
        }
        return exchangeRateManager.get(value, getCurrentFiatCurrency());
    }

    public CurrencyValue getAsFiatValue(CurrencyValue value){
      if (value == null){
         return null;
      }
      if (currentFiatCurrency == null) {
         return null;
      }
      return CurrencyValue.fromValue(value, getCurrentFiatCurrency().getSymbol(), exchangeRateManager);
   }

   public CurrencyValue getAsValue(CurrencyValue value){
      if (value == null){
         return null;
      }
      return CurrencyValue.fromValue(value, getCurrentCurrency().getSymbol(), exchangeRateManager);
   }

   /**
    * Get the exchange rate price for the currently selected currency.
    * <p>
    * Returns null if the current rate is too old or for a different currency.
    * In that the case the caller could choose to call refreshRates() and supply a handler to get a callback.
    */
   public synchronized Double getExchangeRatePrice() {
      ExchangeRate rate = exchangeRateManager.getExchangeRate(currentFiatCurrency);
      return rate == null ? null : rate.price;
   }

    public Value getValue(ValueSum sum) {
        Value result = Value.zeroValue(getCurrentCurrency());
        for (Value value : sum.getValues()) {
            Value value1 = exchangeRateManager.get(value, result.type);
            if (value1 != null) {
                result = result.add(value1);
            }
        }
        return result;
    }
}
