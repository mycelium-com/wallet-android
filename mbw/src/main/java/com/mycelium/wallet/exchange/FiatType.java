package com.mycelium.wallet.exchange;

import com.mycelium.wapi.wallet.MonetaryFormat;
import com.mycelium.wapi.wallet.coins.Value;
import com.mycelium.wapi.wallet.coins.ValueType;

import java.util.Objects;

public class FiatType implements ValueType {
    private String symbol;

    public FiatType(String symbol) {
        this.symbol = symbol;
    }

    @Override
    public String getId() {
        return symbol;
    }

    @Override
    public String getName() {
        return symbol;
    }

    @Override
    public String getSymbol() {
        return symbol;
    }

    @Override
    public int getUnitExponent() {
        return 2;
    }

    @Override
    public Value oneCoin() {
        return null;
    }

    @Override
    public Value getMinNonDust() {
        return null;
    }

    @Override
    public Value value(long units) {
        return null;
    }

    @Override
    public MonetaryFormat getMonetaryFormat() {
        return new MonetaryFormat()
                .shift(0).minDecimals(2).code(0, symbol).postfixCode();
    }

    @Override
    public MonetaryFormat getPlainFormat() {
        return new MonetaryFormat().shift(0)
                .minDecimals(0).repeatOptionalDecimals(1, getUnitExponent()).noCode();
    }

    @Override
    public boolean equals(ValueType o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FiatType fiatType = (FiatType) o;
        return Objects.equals(symbol, fiatType.symbol);
    }

    @Override
    public int hashCode() {

        return Objects.hash(symbol);
    }

    @Override
    public Value value(String string) {
        return null;
    }
}
