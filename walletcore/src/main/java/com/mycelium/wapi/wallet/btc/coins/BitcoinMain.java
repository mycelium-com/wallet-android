package com.mycelium.wapi.wallet.btc.coins;

import com.mrd.bitlib.model.Address;
import com.mycelium.wapi.wallet.GenericAddress;
import com.mycelium.wapi.wallet.btc.BtcAddress;
import com.mycelium.wapi.wallet.coins.CryptoCurrency;

public class BitcoinMain extends CryptoCurrency {
    private BitcoinMain() {
        super("bitcoin.main", "Bitcoin", "BTC", 8, 2, true);
    }

    private static BitcoinMain instance = new BitcoinMain();
    public static synchronized CryptoCurrency get() {
        return instance;
    }

    @Override
    public GenericAddress parseAddress(String addressString) {
        Address address = Address.fromString(addressString);
        if (address == null) {
            return null;
        }

        try {
            if (!address.getNetwork().isProdnet()) {
                return null;
            }
        } catch (IllegalStateException e) {
            return null;
        }
        return new BtcAddress(this, address);
    }
}