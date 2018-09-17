package com.mycelium.wapi.wallet;

import com.mycelium.wapi.wallet.coins.CryptoCurrency;

public interface GenericAddress {
    CryptoCurrency getCoinType();
    String toString();
    long getId();
    String toMultiLineString();
    String toDoubleLineString();
}
