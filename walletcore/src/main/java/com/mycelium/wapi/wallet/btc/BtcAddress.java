package com.mycelium.wapi.wallet.btc;

import com.mrd.bitlib.model.Address;
import com.mycelium.wapi.wallet.GenericAddress;
import com.mycelium.wapi.wallet.coins.CryptoCurrency;

import java.util.Currency;

public class BtcAddress extends Address implements GenericAddress {

    CryptoCurrency currencyType;

    public BtcAddress(CryptoCurrency currencyType, byte[] bytes) {
        super(bytes);
        this.currencyType = currencyType;
    }

    @Override
    public CryptoCurrency getCoinType() {
        return currencyType;
    }

    @Override
    public long getId() {
        return 0;
    }

    public static BtcAddress from(CryptoCurrency currencyType, String address) {
        Address addr = Address.fromString(address);
        return new BtcAddress(currencyType, addr.getAllAddressBytes());
    }
}
