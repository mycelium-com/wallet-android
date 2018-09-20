package com.mycelium.wapi.wallet.btc;

import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.AddressType;
import com.mrd.bitlib.model.NetworkParameters;
import com.mycelium.wapi.wallet.GenericAddress;
import com.mycelium.wapi.wallet.coins.BitcoinMain;
import com.mycelium.wapi.wallet.coins.BitcoinTest;
import com.mycelium.wapi.wallet.coins.CryptoCurrency;

public class BtcAddress extends Address implements GenericAddress {

    CryptoCurrency currencyType;

    public BtcAddress(CryptoCurrency currencyType, byte[] bytes) {
        super(bytes);
        this.currencyType = currencyType;
    }

    @Override
    public AddressType getType() {
        NetworkParameters networkParameters;
        if(currencyType instanceof BitcoinTest)
            networkParameters = NetworkParameters.testNetwork;
        else if(currencyType instanceof BitcoinMain)
            networkParameters = NetworkParameters.productionNetwork;
        else
            networkParameters = NetworkParameters.regtestNetwork;

        if (isP2SH(networkParameters)) {
            return AddressType.P2SH_P2WPKH;
        } else {
            return AddressType.P2PKH;
        }
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
    public String toDoubleLineString() {
        String address = toString();
        int splitIndex = address.length() / 2;
        return address.substring(0, splitIndex) + "\r\n" +
                address.substring(splitIndex);
    }

    @Override
    public String toShortString() {
        int showChars = 3;
        String addressString = toString();
        return addressString.substring(0, showChars) + "..." + addressString.substring(addressString.length() - showChars);
    }

    @Override
    public String toMultiLineString() {
        String address = toString();
        return address.substring(0, 12) + "\r\n" +
                address.substring(12, 24) + "\r\n" +
                address.substring(24);
    }
}
