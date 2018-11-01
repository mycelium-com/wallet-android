package com.mycelium.wapi.wallet.eth;

import com.mrd.bitlib.model.AddressType;
import com.mycelium.wapi.wallet.GenericAddress;
import com.mycelium.wapi.wallet.coins.CryptoCurrency;
import com.mycelium.wapi.wallet.eth.coins.EthMain;

import org.jetbrains.annotations.NotNull;

public class EthAddress implements GenericAddress {

    private String address;

    EthAddress(String address) {
        this.address = address;
    }

    @Override
    public CryptoCurrency getCoinType() {
        return EthMain.INSTANCE;
    }

    @Override
    public long getId() {
        return 0;
    }

    public static EthAddress from(CryptoCurrency currencyType, String address) {
        return new EthAddress(address);
    }

    @NotNull
    @Override
    public byte[] getBytes() {
        return new byte[0];
    }
}
