package com.mycelium.wapi.wallet.fiat;

import com.mycelium.wapi.wallet.GenericAddress;
import com.mycelium.wapi.wallet.coins.CoinType;

public class FiatAddress implements GenericAddress {

    @Override
    public CoinType getCoinType() {
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
}
