package com.mycelium.wapi.wallet.btc;

import com.mrd.bitlib.model.Address;
import com.mycelium.wapi.wallet.AbstractAddress;
import com.mycelium.wapi.wallet.coins.CoinType;

public class BtcAddress extends Address implements AbstractAddress {

    public BtcAddress(byte[] bytes) {
        super(bytes);
    }

    public BtcAddress(byte[] bytes, String stringAddress) {
        super(bytes, stringAddress);
    }

    @Override
    public CoinType getType() {
        return null;
    }

    @Override
    public long getId() {
        return 0;
    }

    public static BtcAddress from(String address) {
        return (BtcAddress)Address.fromString(address);
    }
}
