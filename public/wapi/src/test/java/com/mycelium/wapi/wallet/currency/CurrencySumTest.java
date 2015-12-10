package com.mycelium.wapi.wallet.currency;

import com.google.common.collect.ImmutableMap;
import com.mycelium.wapi.model.ExchangeRate;
import com.mycelium.wapi.wallet.currency.test.AssertHelper;
import junit.framework.AssertionFailedError;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class CurrencySumTest {
   private ExchangeRateProvider fx = new ExchangeRateProvider() {
      private Map<String, Double> fx = new ImmutableMap.Builder<String, Double>()
            .put("USD", 7.0)
            .put("EUR", 5.0)
            .build();

      public ExchangeRate getExchangeRate(String currency) {
         return new ExchangeRate("TEST", 1000L, fx.get(currency), currency);
      }
   };

   private ExchangeRateProvider fxLater = new ExchangeRateProvider() {
      private Map<String, Double> fx = new ImmutableMap.Builder<String, Double>()
            .put("USD", 14.0)
            .put("EUR", 10.0)
            .build();

      public ExchangeRate getExchangeRate(String currency) {
         return new ExchangeRate("TEST", 1000L, fx.get(currency), currency);
      }
   };

   @Test
   public void testSumOneType() throws Exception {
      ExactCurrencyValue b1 = ExactCurrencyValue.from(BigDecimal.ONE, CurrencyValue.BTC);
      ExactCurrencyValue b2 = ExactCurrencyValue.from(BigDecimal.valueOf(2L), CurrencyValue.BTC);

      CurrencySum currencySum = new CurrencySum();
      currencySum.add(b1);
      currencySum.add(b2);

      assertEquals(1, currencySum.getAllValues().size());
      assertEquals(BigDecimal.valueOf(3), currencySum.getAllValues().get(CurrencyValue.BTC).getValue());
   }

   @Test
   public void testSumMoreTypes() throws Exception {
      ExactCurrencyValue b1 = ExactCurrencyValue.from(BigDecimal.ONE, CurrencyValue.BTC);
      ExactCurrencyValue b2 = ExactCurrencyValue.from(BigDecimal.valueOf(2L), CurrencyValue.BTC);
      ExactCurrencyValue f1 = ExactFiatValue.from(BigDecimal.valueOf(3L), "USD");
      ExactCurrencyValue f2 = ExactFiatValue.from(BigDecimal.valueOf(4L), "USD");
      ExactCurrencyValue f3 = ExactFiatValue.from(BigDecimal.valueOf(5L), "EUR");

      CurrencySum currencySum = new CurrencySum();
      currencySum.add(b1);
      currencySum.add(b2);
      currencySum.add(f1);
      currencySum.add(f2);
      currencySum.add(f3);

      assertEquals("We should have 3 currencies: BTC, USD and EUR", 3, currencySum.getAllValues().size());
      assertEquals(BigDecimal.valueOf(3), currencySum.getAllValues().get(CurrencyValue.BTC).getValue());
      assertEquals(BigDecimal.valueOf(7), currencySum.getAllValues().get("USD").getValue());
      assertEquals(BigDecimal.valueOf(5), currencySum.getAllValues().get("EUR").getValue());
   }

   @Test
   public void testGetSumAsCurrency() throws Exception {
      // 1 BTC = 7 USD = 5 EUR
      // b1 + b2 = 3 BTC
      // f1 + f2 = 7 USD = 1 BTC
      // f3 = 5 EUR = 1 BTC
      // sum = 5 BTC = 35 USD
      ExactCurrencyValue b1 = ExactCurrencyValue.from(BigDecimal.ONE, CurrencyValue.BTC);
      ExactCurrencyValue b2 = ExactCurrencyValue.from(BigDecimal.valueOf(2L), CurrencyValue.BTC);
      ExactCurrencyValue f1 = ExactFiatValue.from(BigDecimal.valueOf(3L), "USD");
      ExactCurrencyValue f2 = ExactFiatValue.from(BigDecimal.valueOf(4L), "USD");
      ExactCurrencyValue f3 = ExactFiatValue.from(BigDecimal.valueOf(5L), "EUR");

      CurrencySum currencySum = new CurrencySum();
      currencySum.add(b1);
      currencySum.add(b2);
      currencySum.add(f1);
      currencySum.add(f2);
      currencySum.add(f3);

      CurrencyValue sumAsCurrency = currencySum.getSumAsCurrency(CurrencyValue.BTC, fx);
      AssertHelper.assertRoundedEqualValue(BigDecimal.valueOf(5), sumAsCurrency.getValue(), 8);
      assertEquals(sumAsCurrency.getCurrency(), "BTC");

      CurrencyValue sumAsCurrencyUSD = currencySum.getSumAsCurrency("USD", fx);
      AssertHelper.assertRoundedEqualValue(BigDecimal.valueOf(35), sumAsCurrencyUSD.getValue(), 8);
      assertEquals("USD", sumAsCurrencyUSD.getCurrency());
   }

   @Test
   public void testGetSumAsCurrencyWithFxBasedValues() throws Exception {
      ExactCurrencyValue b1 = ExactCurrencyValue.from(BigDecimal.ONE, CurrencyValue.BTC);
      ExactCurrencyValue b2 = ExactCurrencyValue.from(BigDecimal.valueOf(2L), CurrencyValue.BTC);
      ExactCurrencyValue f1 = ExactFiatValue.from(BigDecimal.valueOf(3L), "USD");
      ExactCurrencyValue f2 = ExactFiatValue.from(BigDecimal.valueOf(4L), "USD");
      ExactCurrencyValue f3 = ExactFiatValue.from(BigDecimal.valueOf(5L), "EUR");

      CurrencySum currencySum = new CurrencySum();
      currencySum.add(b1);
      currencySum.add(b2);
      currencySum.add(ExchangeBasedCurrencyValue.fromExactValue(f1, "BTC", fx));
      currencySum.add(ExchangeBasedCurrencyValue.fromExactValue(f2, "BTC", fx));
      currencySum.add(ExchangeBasedCurrencyValue.fromExactValue(f3, "BTC", fx));

      CurrencyValue sumAsCurrency = currencySum.getSumAsCurrency(CurrencyValue.BTC, fx);
      AssertHelper.assertRoundedEqualValue(BigDecimal.valueOf(5), sumAsCurrency.getValue(), 7);
      assertEquals(sumAsCurrency.getCurrency(), "BTC");

      // use a different FX rate, it should always use the exact values for added currencies
      // 1 BTC -> 14 USD
      // 2 BTC -> 28 USD
      // 3 USD -> 3 USD
      // 4 USD -> 4 USD
      // 5 EUR -> 0.5 BTC -> 7 USD
      // -> 56 USD
      CurrencyValue sumAsCurrencyUSD = currencySum.getSumAsCurrency("USD", fxLater);
      AssertHelper.assertRoundedEqualValue(BigDecimal.valueOf(56), sumAsCurrencyUSD.getValue(), 7);
      assertEquals("USD", sumAsCurrencyUSD.getCurrency());
   }
}
