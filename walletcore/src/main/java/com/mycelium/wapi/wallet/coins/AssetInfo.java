package com.mycelium.wapi.wallet.coins;

import com.mycelium.wapi.wallet.Address;

import java.io.Serializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface AssetInfo extends Serializable {
    @Nonnull String getId();
    @Nonnull String getName();
    @Nonnull String getSymbol();
    int getUnitExponent();
    int getFriendlyDigits();

    /**
     * Typical 1 coin value, like 1 Bitcoin, 1 Peercoin or 1 Dollar
     */
    @Nonnull Value oneCoin();

    @Nonnull Value value(long units);

    @Nonnull Value value(@Nonnull String string);

    Address parseAddress(@Nullable String address);
}
