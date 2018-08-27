package com.mycelium.wapi.wallet.eth;

import com.mycelium.wapi.wallet.GenericAddress;
import com.mycelium.wapi.wallet.coins.CoinType;

public class EthAddress implements GenericAddress {

    @Override
    public CoinType getCoinType() {
        return null;
    }

    @Override
    public long getId() {
        return 0;
    }
}
