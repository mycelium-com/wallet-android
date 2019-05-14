package com.mycelium.wapi.wallet.fiat;

import com.mrd.bitlib.model.AddressType;
import com.mycelium.wapi.wallet.GenericAddress;
import com.mycelium.wapi.wallet.coins.CryptoCurrency;
import org.jetbrains.annotations.NotNull;

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

    public static FiatAddress from(CryptoCurrency currencyType, String address) {
        return null;
    }

    @NotNull
    @Override
    public byte[] getBytes() {
        return new byte[0];
    }

    @NotNull
    @Override
    public String getSubType() {
        return "";
    }
}
