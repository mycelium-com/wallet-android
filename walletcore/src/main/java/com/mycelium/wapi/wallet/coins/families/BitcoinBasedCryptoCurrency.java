package com.mycelium.wapi.wallet.coins.families;

import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.AddressType;
import com.mrd.bitlib.model.NetworkParameters;
import com.mycelium.wapi.wallet.AddressUtils;
import com.mycelium.wapi.wallet.GenericAddress;
import com.mycelium.wapi.wallet.btc.BtcLegacyAddress;
import com.mycelium.wapi.wallet.btc.coins.BitcoinMain;
import com.mycelium.wapi.wallet.btc.coins.BitcoinTest;
import com.mycelium.wapi.wallet.coins.CryptoCurrency;
import com.mycelium.wapi.wallet.exceptions.AddressMalformedException;
import com.mycelium.wapi.wallet.segwit.SegwitAddress;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class BitcoinBasedCryptoCurrency extends CryptoCurrency {
    {
        family = Families.BITCOIN;
    }

    @Override
    public GenericAddress parseAddress(String addressString) {
        Address address = Address.fromString(addressString);
        if (address == null) {
            return null;
        }
        return (address.getType() == AddressType.P2WPKH) ?
                new SegwitAddress((com.mrd.bitlib.model.SegwitAddress) address) :
                new BtcLegacyAddress(address.getNetwork().isProdnet() ? BitcoinMain.get() : BitcoinTest.get(),
                        address.getAllAddressBytes());
    }
}
