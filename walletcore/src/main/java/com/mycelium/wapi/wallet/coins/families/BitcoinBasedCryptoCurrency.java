package com.mycelium.wapi.wallet.coins.families;

import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;
import com.mycelium.wapi.wallet.AddressUtils;
import com.mycelium.wapi.wallet.GenericAddress;
import com.mycelium.wapi.wallet.coins.CryptoCurrency;
import com.mycelium.wapi.wallet.exceptions.AddressMalformedException;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class BitcoinBasedCryptoCurrency extends CryptoCurrency {
    {
        family = Families.BITCOIN;
    }

    public GenericAddress newAddress(String addressStr) {
        return AddressUtils.from(this, addressStr);
    }

    @Override
    public String getName() {
        return "Bitcoin";
    }

    @Override
    public boolean isMineAddress(String address) {
        try {
            return Address.fromString(address) != null;
        } catch(Exception ex) {
            return false;
        }
    }
}
