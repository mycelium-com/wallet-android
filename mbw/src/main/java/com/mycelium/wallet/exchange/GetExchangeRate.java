package com.mycelium.wallet.exchange;

import com.mycelium.wapi.model.ExchangeRate;
import com.mycelium.wapi.wallet.btc.coins.BitcoinMain;
import com.mycelium.wapi.wallet.btc.coins.BitcoinTest;
import com.mycelium.wapi.wallet.currency.ExchangeRateProvider;
import com.mycelium.wapi.wallet.eth.coins.EthCoin;
import com.mycelium.wapi.wallet.eth.coins.EthMain;
import com.mycelium.wapi.wallet.eth.coins.EthTest;

import java.math.BigDecimal;
import java.math.RoundingMode;

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

        if (sourceCurrency.equals(BitcoinMain.get().getSymbol())
                || sourceCurrency.equals(BitcoinTest.get().getSymbol())
                || sourceCurrency.equals(EthMain.INSTANCE.getSymbol())
                || sourceCurrency.equals(EthTest.INSTANCE.getSymbol())) {
            sourcePrice = BigDecimal.ONE;
        } else {
            sourceExchangeRate = exchangeRateManager.getExchangeRate(targetCurrency, sourceCurrency);
            if (sourceExchangeRate != null && sourceExchangeRate.price != null) {
                sourcePrice = BigDecimal.valueOf(sourceExchangeRate.price);
            }
        }

        if (targetCurrency.equals(BitcoinMain.get().getSymbol())
                || targetCurrency.equals(BitcoinTest.get().getSymbol())
                || targetCurrency.equals(EthMain.INSTANCE.getSymbol())
                || targetCurrency.equals(EthTest.INSTANCE.getSymbol())) {
            targetPrice = BigDecimal.ONE;
        } else {
            targetExchangeRate = exchangeRateManager.getExchangeRate(sourceCurrency, targetCurrency);
            if (targetExchangeRate != null && targetExchangeRate.price != null) {
                targetPrice = BigDecimal.valueOf(targetExchangeRate.price);
            }
        }
        return this;
    }
}
