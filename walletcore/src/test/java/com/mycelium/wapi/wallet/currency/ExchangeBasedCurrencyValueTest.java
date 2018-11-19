package com.mycelium.wapi.wallet.currency;

import com.google.common.collect.ImmutableMap;
import com.mycelium.wapi.model.ExchangeRate;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.math.BigDecimal.valueOf;

import static org.junit.Assert.*;

public class ExchangeBasedCurrencyValueTest {
   private ExchangeRateProvider fx = new ExchangeRateProvider() {
      private Map<String, Double> fx = new ImmutableMap.Builder<String, Double>()
            .put("USD", 7.0)
            .put("EUR", 5.0)
            .build();

      public ExchangeRate getExchangeRate(String currency) {
         return new ExchangeRate("TEST", 1000L, fx.get(currency), currency);
      }
   };

   @Test
   public void testFromValue() throws Exception {

   }

   @Test
   public void testFromValue1() throws Exception {

   }

   @Test
   public void testFromValue2() throws Exception {

   }

   @Test
   public void testFrom() throws Exception {

   }

   @Test
   public void testConvertFromValue() throws Exception {

   }

   @Test
   public void testFromExactValueToBtc() throws Exception {
      List<ExactCurrencyValue> fiatSources = new ArrayList<ExactCurrencyValue>();
      List<BigDecimal> expectedBtcs = new ArrayList<BigDecimal>();

      fiatSources.add(ExactBitcoinValue.from(valueOf(123L, 4)));
      expectedBtcs.add(valueOf(123L, 4));

      fiatSources.add(ExactFiatValue.from(valueOf(7L), "USD"));
      expectedBtcs.add(BigDecimal.ONE);

      fiatSources.add(ExactFiatValue.from(valueOf(1L), "USD"));
      expectedBtcs.add(valueOf(14285714L, 8));

      fiatSources.add(ExactFiatValue.from(valueOf(5L), "EUR"));
      expectedBtcs.add(BigDecimal.ONE);

      fiatSources.add(ExactFiatValue.from(valueOf(1L), "EUR"));
      expectedBtcs.add(valueOf(2L, 1));

      for(int i=0; i<fiatSources.size(); i++) {
         ExactCurrencyValue fiatSource = fiatSources.get(i);
         BigDecimal expectedBtc = expectedBtcs.get(i);
         // method under test: fromExactValue
         BigDecimal actualBtcs = ExchangeBasedCurrencyValue.fromExactValue(fiatSource, "BTC", fx).getValue();
         String message = fiatSource + " should be " + expectedBtc + " BTC.";
         assertEquals(message, expectedBtc, actualBtcs);
      }
   }
}
