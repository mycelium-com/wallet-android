package com.mycelium.wapi.wallet.currency;


import java.math.BigDecimal;

public class ExactBitcoinCashValue extends ExactBitcoinValue {
    public static final ExactCurrencyValue ZERO = from(0L);

    public static ExactBitcoinCashValue from(BigDecimal value) {
        return new ExactBitcoinCashValue(value);
    }
    public static ExactBitcoinCashValue from(Long value) {
        return new ExactBitcoinCashValue(value);
    }


    protected ExactBitcoinCashValue(Long satoshis) {
        super(satoshis);
    }

    protected ExactBitcoinCashValue(BigDecimal bitcoins) {
        super(bitcoins);
    }

    @Override
    public String getCurrency() {
        return BCH;
    }
}
