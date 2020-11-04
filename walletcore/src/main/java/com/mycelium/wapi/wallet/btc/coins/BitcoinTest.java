package com.mycelium.wapi.wallet.btc.coins;

import com.mrd.bitlib.model.BitcoinAddress;
import com.mycelium.wapi.wallet.Address;
import com.mycelium.wapi.wallet.btc.BtcAddress;
import com.mycelium.wapi.wallet.coins.CryptoCurrency;

public class BitcoinTest extends CryptoCurrency {
    private BitcoinTest() {
        super("bitcoin.test", "Bitcoin Test", "tBTC", 8, 2, true);
    }

    private static BitcoinTest instance = new BitcoinTest();
    public static synchronized CryptoCurrency get() {
        return instance;
    }

    @Override
    public Address parseAddress(String addressString) {
        BitcoinAddress address = BitcoinAddress.fromString(addressString);
        if (address == null) {
            return null;
        }

        try {
            if (!address.getNetwork().isTestnet()) {
                return null;
            }
        } catch (IllegalStateException e) {
            return null;
        }
        return new BtcAddress(this, address);
    }
}