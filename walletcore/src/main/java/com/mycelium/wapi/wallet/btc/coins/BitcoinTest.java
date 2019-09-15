package com.mycelium.wapi.wallet.btc.coins;

import com.mrd.bitlib.model.Address;
import com.mycelium.wapi.wallet.GenericAddress;
import com.mycelium.wapi.wallet.btc.BtcAddress;
import com.mycelium.wapi.wallet.coins.CryptoCurrency;
import com.mycelium.wapi.wallet.coins.families.BitcoinBasedCryptoCurrency;

public class BitcoinTest extends BitcoinBasedCryptoCurrency {
    private BitcoinTest() {
        id = "bitcoin.test";

        name = "Bitcoin Test";
        // TODO temporary tBTC -> BTC
        symbol = "BTC";
        unitExponent = 8;
    }

    private static BitcoinTest instance = new BitcoinTest();
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
            if (!address.getNetwork().isTestnet()) {
                return null;
            }
        } catch (IllegalStateException e) {
            return null;
        }
        return new BtcAddress(this, address);
    }
}