package com.mycelium.wapi.wallet.fiat;

import com.mycelium.wapi.wallet.GenericAddress;
import com.mycelium.wapi.wallet.coins.CryptoCurrency;

public class FiatAddress implements GenericAddress {

    @Override
    public CryptoCurrency getCoinType() {
        return null;
    }

    @Override
    public String toString() {
        return null;
    }

    @Override
    public long getId() {
        return 0;
    }

    @Override
    public String toDoubleLineString() {
        return null;
    }

    @Override
    public String toShortString() {
        return null;
    }

    @Override
    public String toMultiLineString() {
        return null;
    }
}
