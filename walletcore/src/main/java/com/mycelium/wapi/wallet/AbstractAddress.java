package com.mycelium.wapi.wallet;

import com.mycelium.wapi.wallet.coins.CoinType;

public interface AbstractAddress {
    CoinType getType();
    String toString();
    long getId();
}
