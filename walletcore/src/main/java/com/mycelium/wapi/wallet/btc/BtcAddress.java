package com.mycelium.wapi.wallet.btc;

import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.AddressType;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.model.hdpath.HdKeyPath;
import com.mycelium.wapi.wallet.GenericAddress;
import com.mycelium.wapi.wallet.coins.BitcoinMain;
import com.mycelium.wapi.wallet.coins.BitcoinTest;
import com.mycelium.wapi.wallet.coins.CryptoCurrency;

public class BtcAddress implements GenericAddress {

    private Address address;
    private CryptoCurrency currencyType;

    public BtcAddress(CryptoCurrency currencyType, byte[] bytes) {
        address = new Address(bytes);
        this.currencyType = currencyType;
    }

    public AddressType getType() {
        NetworkParameters networkParameters;
        if(currencyType instanceof BitcoinTest)
            networkParameters = NetworkParameters.testNetwork;
        else if(currencyType instanceof BitcoinMain)
            networkParameters = NetworkParameters.productionNetwork;
        else
            networkParameters = NetworkParameters.regtestNetwork;

        if (address.isP2SH(networkParameters)) {
            return AddressType.P2SH_P2WPKH;
        } else {
            return AddressType.P2PKH;
        }
    }

    public Address getAddress() {
        return address;
    }

    @Override
    public String toString(){
        return address.toString();
    }

    @Override
    public CryptoCurrency getCoinType() {
        return currencyType;
    }

    @Override
    public long getId() {
        return 0;
    }

    @Override
    public HdKeyPath getBip32Path() {
        return address.getBip32Path();
    }

}
