package com.mycelium.wapi.wallet.eth;

import com.mrd.bitlib.model.AddressType;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.model.hdpath.HdKeyPath;
import com.mycelium.wapi.wallet.GenericAddress;
import com.mycelium.wapi.wallet.coins.CryptoCurrency;

public class EthAddress implements GenericAddress {

    @Override
    public CryptoCurrency getCoinType() {
        return null;
    }

    @Override
    public AddressType getType() {
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

    public static EthAddress from(CryptoCurrency currencyType, String address) {
        return null;
    }
}
