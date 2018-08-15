package com.mycelium.wapi.wallet;

import com.mycelium.wapi.wallet.coins.CoinType;

public interface AbstractAddress {
    CoinType getCoinType();
    String toString();
    long getId();
}
