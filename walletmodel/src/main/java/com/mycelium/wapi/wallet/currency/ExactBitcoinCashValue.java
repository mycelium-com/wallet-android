package com.mycelium.wapi.wallet.currency;


import com.megiontechnologies.BitcoinCash;

import java.math.BigDecimal;

public class ExactBitcoinCashValue extends ExactCurrencyValue {
    private final BitcoinCash value;
    public static final ExactCurrencyValue ZERO = from(0L);
    public static final ExactCurrencyValue ONE = from(BigDecimal.ONE);

    public static ExactBitcoinCashValue from(BigDecimal value) {
        return new ExactBitcoinCashValue(value);
    }
    public static ExactBitcoinCashValue from(Long value) {
        return new ExactBitcoinCashValue(value);
    }
    public static ExactBitcoinCashValue from(BitcoinCash value) {
        return new ExactBitcoinCashValue(value);
    }

    protected ExactBitcoinCashValue(Long satoshis) {
        if (satoshis != null) {
            value = BitcoinCash.valueOf(satoshis);
        } else {
            value = null;
        }
    }

    protected ExactBitcoinCashValue(BitcoinCash bitcoins) {
        value = bitcoins;
    }


    protected ExactBitcoinCashValue(BigDecimal bitcoins) {
        if (bitcoins != null) {
            value = BitcoinCash.nearestValue(bitcoins);
        } else {
            value = null;
        }
    }

    public long getLongValue() {
        return getAsBitcoinCash().getLongValue();
    }

    public BitcoinCash getAsBitcoinCash() {
        return value;
    }

    @Override
    public String getCurrency() {
        return CurrencyValue.BCH;
    }

    @Override
    public BigDecimal getValue() {
        if (value != null) {
            return value.toBigDecimal();
        } else {
            return null;
        }
    }
}
