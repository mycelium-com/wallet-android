package com.mycelium.wapi.wallet.coins.families;

import com.mycelium.wapi.wallet.btc.BtcAddress;
import com.mycelium.wapi.wallet.coins.CoinType;
import com.mycelium.wapi.wallet.exceptions.AddressMalformedException;

public abstract class BitFamily extends CoinType {
    {
        family = Families.BITCOIN;
    }

    @Override
    public BtcAddress newAddress(String addressStr) throws AddressMalformedException {
        return BtcAddress.from(addressStr);
    }
}
