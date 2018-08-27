package com.mycelium.wapi.wallet.btc;

import com.mrd.bitlib.model.Address;
import com.mycelium.wapi.wallet.GenericAddress;
import com.mycelium.wapi.wallet.coins.CoinType;

public class BtcAddress extends Address implements GenericAddress {

    public BtcAddress(byte[] bytes) {
        super(bytes);
    }

    public BtcAddress(byte[] bytes, String stringAddress) {
        super(bytes, stringAddress);
    }

    @Override
    public CoinType getCoinType() {
        return null;
    }

    @Override
    public long getId() {
        return 0;
    }

    public static BtcAddress from(String address) {
        return new BtcAddress(Address.fromString(address).getAllAddressBytes());
    }
}
