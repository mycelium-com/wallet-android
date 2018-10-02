package com.mycelium.wapi.wallet.eth;

import com.mrd.bitlib.model.NetworkParameters;
import com.mycelium.wapi.wallet.GenericAddress;
import com.mycelium.wapi.wallet.coins.CryptoCurrency;

public class EthAddress implements GenericAddress {

    @Override
    public CryptoCurrency getCoinType() {
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
    public byte[] getAllAddressBytes() {
        return new byte[0];
    }

    @Override
    public String toMultiLineString() {
        return null;
    }

    public static EthAddress from(CryptoCurrency currencyType, String address) {
        return null;
    }
}
