package com.mycelium.wapi.wallet.fiat;

import com.mrd.bitlib.model.AddressType;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.model.hdpath.HdKeyPath;
import com.mycelium.wapi.wallet.GenericAddress;
import com.mycelium.wapi.wallet.coins.CryptoCurrency;

public class FiatAddress implements GenericAddress {

    @Override
    public CryptoCurrency getCoinType() {
        return null;
    }

    @Override
    public AddressType getType() {
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
    public HdKeyPath getBip32Path() {
        return null;
    }

    public static FiatAddress from(CryptoCurrency currencyType, String address) {
        return null;
    }
}
