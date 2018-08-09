package com.mycelium.wapi.wallet.coins;

import com.mycelium.wapi.wallet.MonetaryFormat;

public interface ValueType {
    String getId();
    String getName();
    String getSymbol();
    int getUnitExponent();

    /**
     * Typical 1 coin value, like 1 Bitcoin, 1 Peercoin or 1 Dollar
     */
    Value oneCoin();

    /**
     * Get the minimum valid amount that can be sent a.k.a. dust amount or minimum input
     */
    Value getMinNonDust();

    Value value(long units);

    MonetaryFormat getMonetaryFormat();
    MonetaryFormat getPlainFormat();

    boolean equals(ValueType obj);

    Value value(String string);
}
