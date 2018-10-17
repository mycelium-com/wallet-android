package com.mycelium.wapi.wallet.eth;

import com.mrd.bitlib.model.AddressType;
import com.mycelium.wapi.wallet.GenericAddress;
import com.mycelium.wapi.wallet.coins.CryptoCurrency;
import org.jetbrains.annotations.NotNull;

public class EthAddress implements GenericAddress {

    @Override
    public CryptoCurrency getCoinType() {
        return null;
    }

    @Override
    public long getId() {
        return 0;
    }

    public static EthAddress from(CryptoCurrency currencyType, String address) {
        return null;
    }

    @NotNull
    @Override
    public AddressType getType() {
        return null;
    }
}
