package com.mycelium.wapi.wallet.coinapult;

import com.google.common.collect.ImmutableMap;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Map;

public class Currency {
    public static final Currency USD = new Currency("USD", BigDecimal.ONE);
    public static final Currency EUR = new Currency("EUR", BigDecimal.ONE);
    public static final Currency GBP = new Currency("GBP", BigDecimal.ONE);
    public static final Currency BTC = new Currency("BTC", BigDecimal.ZERO);
    public static final Map<String, Currency> all = ImmutableMap.of(
            USD.name, USD,
            EUR.name, EUR,
            GBP.name, GBP,
            BTC.name, BTC
    );

    final public String name;
    final public BigDecimal minimumConversationValue;

    private Currency(String name, BigDecimal minimumConversationValue) {
        this.name = name;
        this.minimumConversationValue = minimumConversationValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Currency currency = (Currency) o;
        return name.equals(currency.name) && minimumConversationValue.equals(currency.minimumConversationValue);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + minimumConversationValue.hashCode();
        return result;
    }

    public String getMinimumConversationString() {
        return new DecimalFormat("#0.00##").format(minimumConversationValue) + " " + name;
    }
}
