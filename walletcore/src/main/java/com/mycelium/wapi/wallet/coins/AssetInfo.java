package com.mycelium.wapi.wallet.coins;

import com.mycelium.wapi.wallet.Address;

import java.io.Serializable;

public interface AssetInfo extends Serializable {
    String getId();
    String getName();
    String getSymbol();
    int getUnitExponent();
    int getFriendlyDigits();

    /**
     * Typical 1 coin value, like 1 Bitcoin, 1 Peercoin or 1 Dollar
     */
    Value oneCoin();

    Value value(long units);

    Value value(String string);

    boolean isMineAddress(String address);

    Address parseAddress(String address);
}
