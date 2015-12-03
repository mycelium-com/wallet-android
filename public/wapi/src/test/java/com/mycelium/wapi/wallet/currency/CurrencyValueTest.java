package com.mycelium.wapi.wallet.currency;

import com.mycelium.wapi.model.ExchangeRate;
import junit.framework.TestCase;

import java.math.BigDecimal;

public class CurrencyValueTest extends TestCase {
   ExchangeRateProvider fakeExchangeRate = new ExchangeRateProvider() {
      @Override
      public ExchangeRate getExchangeRate(String currency) {
         if (currency.equals("USD")) {
            return new ExchangeRate("FAKE", 0, 10, "USD"); // 1 BTC costs 10 USD
         } else if (currency.equals("EUR")) {
            return new ExchangeRate("FAKE", 0, 20, "EUR"); // 1 BTC costs 20 EUR
         }
         return null;
      }
   };

   ExchangeRateProvider fakeExchangeRate2 = new ExchangeRateProvider() {
      @Override
      public ExchangeRate getExchangeRate(String currency) {
         if (currency.equals("USD")) {
            return new ExchangeRate("FAKE", 0, 100, "USD"); // 1 BTC costs 100 USD
         } else if (currency.equals("EUR")) {
            return new ExchangeRate("FAKE", 0, 400, "EUR"); // 1 BTC costs 400 EUR
         }
         return null;
      }
   };

   // Starting with an exact BTC amount
   public void testExchangeRateHandlingExactBtc() {
      ExactCurrencyValue btc = ExactBitcoinValue.from(BigDecimal.ONE);
      CurrencyValue usd = CurrencyValue.fromValue(btc, "USD", fakeExchangeRate);
      CurrencyValue eur = CurrencyValue.fromValue(btc, "EUR", fakeExchangeRate);
      CurrencyValue btc1 = CurrencyValue.fromValue(btc, CurrencyValue.BTC, fakeExchangeRate2);

      assertTrue(BigDecimal.valueOf(10L).compareTo(usd.getValue()) == 0);
      assertEquals("USD", usd.getCurrency());

      assertTrue(BigDecimal.valueOf(20L).compareTo(eur.getValue()) == 0);
      assertEquals("EUR", eur.getCurrency());

      // it should not have used a fx rate to convert from/to the same currency
      assertTrue(BigDecimal.ONE.compareTo(btc1.getValue()) == 0);
      assertEquals("BTC", btc1.getCurrency());
      assertTrue(btc1 instanceof ExactBitcoinValue);


      // it should return the exact BTC amount, even if the exchange rate has changed
      CurrencyValue btc2 = CurrencyValue.fromValue(eur, CurrencyValue.BTC, fakeExchangeRate2);
      assertTrue(BigDecimal.ONE.compareTo(btc2.getValue()) == 0);

      // converting from a converted EUR amount, it should go back to the known BTC amount and use the current fx rate to get USD
      CurrencyValue usd1 = CurrencyValue.fromValue(eur, "USD", fakeExchangeRate2);
      assertTrue(BigDecimal.valueOf(100L).compareTo(usd1.getValue()) == 0);
   }


   // Starting with an exact Fiat amount
   public void testExchangeRateHandlingExactFiat() {
      ExactCurrencyValue usd = ExactFiatValue.from(BigDecimal.valueOf(10L), "USD");
      CurrencyValue eur = CurrencyValue.fromValue(usd, "EUR", fakeExchangeRate);
      CurrencyValue btc = CurrencyValue.fromValue(usd, CurrencyValue.BTC, fakeExchangeRate);
      CurrencyValue usd1 = CurrencyValue.fromValue(usd, "USD", fakeExchangeRate2);

      assertTrue(BigDecimal.ONE.compareTo(btc.getValue()) == 0);
      assertEquals(CurrencyValue.BTC, btc.getCurrency());

      assertTrue(BigDecimal.valueOf(20L).compareTo(eur.getValue()) == 0);
      assertEquals("EUR", eur.getCurrency());

      // it should not have used a fx rate to convert from/to the same currency
      assertTrue(BigDecimal.valueOf(10L).compareTo(usd1.getValue()) == 0);
      assertEquals("USD", usd1.getCurrency());
      assertTrue(usd1 instanceof ExactFiatValue);

      // it should return the exact Fiat amount, even if the exchange rate has changed
      CurrencyValue usd2 = CurrencyValue.fromValue(btc, "USD", fakeExchangeRate2);
      assertTrue(BigDecimal.valueOf(10L).compareTo(usd2.getValue()) == 0);

      // converting from a converted BTC amount, it should go back to the known USD amount and use the current fx rate to get EUR
      // we have 1 BTC (with base value 10 USD), convert to EUR with 100 USD = 400 EUR -> 40 EUR
      CurrencyValue eur1 = CurrencyValue.fromValue(btc, "EUR", fakeExchangeRate2);
      assertTrue(BigDecimal.valueOf(40L).compareTo(eur1.getValue()) == 0);
   }

   public void testIsNullOrZero() throws Exception {
      assertFalse(CurrencyValue.isNullOrZero(ExactBitcoinValue.from(BigDecimal.ONE)));
      assertFalse(CurrencyValue.isNullOrZero(ExactCurrencyValue.from(BigDecimal.ONE, "EUR")));

      assertTrue(CurrencyValue.isNullOrZero(null));
      assertTrue(CurrencyValue.isNullOrZero(ExactCurrencyValue.from(null, "EUR")));
      assertTrue(CurrencyValue.isNullOrZero(ExactBitcoinValue.from(0L)));
      assertTrue(CurrencyValue.isNullOrZero(ExactBitcoinValue.ZERO));
   }
}