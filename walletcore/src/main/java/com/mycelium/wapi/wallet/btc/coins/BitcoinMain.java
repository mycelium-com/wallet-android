package com.mycelium.wapi.wallet.btc.coins;

import com.mrd.bitlib.model.BitcoinAddress;
import com.mycelium.wapi.wallet.Address;
import com.mycelium.wapi.wallet.btc.BtcAddress;
import com.mycelium.wapi.wallet.coins.CryptoCurrency;
import com.mycelium.wapi.wallet.coins.families.BitcoinBasedCryptoCurrency;

public class BitcoinMain extends BitcoinBasedCryptoCurrency {
    private BitcoinMain() {
        id = "bitcoin.main";

        name = "Bitcoin";
        symbol = "BTC";
        unitExponent = 8;
    }

    private static BitcoinMain instance = new BitcoinMain();
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
            if (!address.getNetwork().isProdnet()) {
                return null;
            }
        } catch (IllegalStateException e) {
            return null;
        }
        return new BtcAddress(this, address);
    }
}