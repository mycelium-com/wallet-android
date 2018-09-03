package com.mycelium.wallet.exchange;

import com.mycelium.wapi.model.ExchangeRate;
import com.mycelium.wapi.wallet.currency.ExchangeRateProvider;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.mycelium.wallet.exchange.ExchangeRateManager.BTC;

public class GetExchangeRate {
      private String targetCurrency;
      private String sourceCurrency;
      private ExchangeRateProvider exchangeRateManager;
      private BigDecimal sourcePrice;
      private BigDecimal targetPrice;
      private ExchangeRate sourceExchangeRate;
      private ExchangeRate targetExchangeRate;

      public GetExchangeRate(String targetCurrency, String sourceCurrency, ExchangeRateProvider exchangeRateManager) {
         this.targetCurrency = targetCurrency;
         this.sourceCurrency = sourceCurrency;
         this.exchangeRateManager = exchangeRateManager;
      }

      // multiply the source value by this rate, to get the target value
      public BigDecimal getRate() {
          if (getTargetPrice() == null || getSourcePrice() == null) {
              return null;
          }
          return getTargetPrice().divide(getSourcePrice(), 10, RoundingMode.HALF_UP);
      }

      public BigDecimal getSourcePrice() {
         return sourcePrice;
      }

      public BigDecimal getTargetPrice() {
         return targetPrice;
      }

      public ExchangeRate getSourceExchangeRate() {
         return sourceExchangeRate;
      }

      public ExchangeRate getTargetExchangeRate() {
         return targetExchangeRate;
      }

      public GetExchangeRate invoke() {
         sourcePrice = null;
         targetPrice = null;
         sourceExchangeRate = null;
         targetExchangeRate = null;

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
         return this;
      }
   }
