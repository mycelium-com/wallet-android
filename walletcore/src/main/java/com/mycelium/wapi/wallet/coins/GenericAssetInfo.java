package com.mycelium.wapi.wallet.coins;

import com.mycelium.wapi.wallet.MonetaryFormat;

import java.io.Serializable;

public interface GenericAssetInfo extends Serializable {
    String getId();
    String getName();
    String getSymbol();
    int getUnitExponent();

    /**
     * Typical 1 coin value, like 1 Bitcoin, 1 Peercoin or 1 Dollar
     */
    Value oneCoin();

    Value value(long units);

    MonetaryFormat getMonetaryFormat();
    MonetaryFormat getPlainFormat();

    //boolean equals(GenericAssetInfo obj);

    Value value(String string);
}
