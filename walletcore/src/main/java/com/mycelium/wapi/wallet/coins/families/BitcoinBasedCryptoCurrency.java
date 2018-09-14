package com.mycelium.wapi.wallet.coins.families;

import com.mycelium.wapi.wallet.btc.BtcAddress;
import com.mycelium.wapi.wallet.coins.CryptoCurrency;
import com.mycelium.wapi.wallet.exceptions.AddressMalformedException;

public abstract class BitcoinBasedCryptoCurrency extends CryptoCurrency {
    {
        family = Families.BITCOIN;
    }

    public BtcAddress newAddress(String addressStr) throws AddressMalformedException {
        return BtcAddress.from(this, addressStr);
    }
}
