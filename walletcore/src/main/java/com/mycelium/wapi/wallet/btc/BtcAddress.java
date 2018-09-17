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

    @Override
    public String toDoubleLineString() {
        String address = toString();
        int splitIndex = address.length() / 2;
        return address.substring(0, splitIndex) + "\r\n" +
                address.substring(splitIndex);
    }

    @Override
    public String toMultiLineString() {
        String address = toString();
        return address.substring(0, 12) + "\r\n" +
                address.substring(12, 24) + "\r\n" +
                address.substring(24);
    }
}
