package com.mycelium.wallet.exchange;

import com.mycelium.wapi.model.ExchangeRate;
import com.mycelium.wapi.wallet.WalletManager;
import com.mycelium.wapi.wallet.btc.coins.BitcoinMain;
import com.mycelium.wapi.wallet.btc.coins.BitcoinTest;
import com.mycelium.wapi.wallet.currency.ExchangeRateProvider;
import com.mycelium.wapi.wallet.eth.coins.EthMain;
import com.mycelium.wapi.wallet.eth.coins.EthTest;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.mycelium.wallet.Utils.isERC20Token;

public class GetExchangeRate {
    private String targetCurrency;
    private String sourceCurrency;
    private ExchangeRateProvider exchangeRateManager;
    private WalletManager walletManager;
    private BigDecimal sourcePrice;
    private BigDecimal targetPrice;
    private ExchangeRate sourceExchangeRate;
    private ExchangeRate targetExchangeRate;

    public GetExchangeRate(WalletManager walletManager, String targetCurrency, String sourceCurrency, ExchangeRateProvider exchangeRateManager) {
        this.targetCurrency = targetCurrency;
        this.sourceCurrency = sourceCurrency;
        this.exchangeRateManager = exchangeRateManager;
        this.walletManager = walletManager;
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

        if (isBtc(sourceCurrency) || isEth(sourceCurrency)
                || (isERC20Token(walletManager, sourceCurrency) && !isEth(targetCurrency))) {
            sourcePrice = BigDecimal.ONE;
        } else {
            sourceExchangeRate = exchangeRateManager.getExchangeRate(targetCurrency, sourceCurrency);
            if (sourceExchangeRate != null && sourceExchangeRate.price != null) {
                sourcePrice = BigDecimal.valueOf(sourceExchangeRate.price);
            }
        }

        if (isBtc(targetCurrency) || isEth(targetCurrency) || isERC20Token(walletManager, targetCurrency)) {
            targetPrice = BigDecimal.ONE;
        } else {
            targetExchangeRate = exchangeRateManager.getExchangeRate(sourceCurrency, targetCurrency);
            if (targetExchangeRate != null && targetExchangeRate.price != null) {
                targetPrice = BigDecimal.valueOf(targetExchangeRate.price);
            }
        }
        return this;
    }

    private boolean isBtc(String currencySymbol) {
        return currencySymbol.equals(BitcoinMain.get().getSymbol()) || currencySymbol.equals(BitcoinTest.get().getSymbol());
    }

    private boolean isEth(String currencySymbol) {
        return currencySymbol.equals(EthMain.INSTANCE.getSymbol()) || currencySymbol.equals(EthTest.INSTANCE.getSymbol());
    }
}
