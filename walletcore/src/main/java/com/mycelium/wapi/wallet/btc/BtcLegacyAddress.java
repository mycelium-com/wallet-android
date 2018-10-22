package com.mycelium.wapi.wallet.btc;

import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.AddressType;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.model.hdpath.HdKeyPath;
import com.mycelium.wapi.wallet.coins.BitcoinMain;
import com.mycelium.wapi.wallet.coins.BitcoinTest;
import com.mycelium.wapi.wallet.coins.CryptoCurrency;
import org.jetbrains.annotations.NotNull;

public class BtcLegacyAddress implements BtcAddress {

    private Address address;
    private CryptoCurrency currencyType;

    public BtcLegacyAddress(CryptoCurrency currencyType, byte[] bytes) {
        address = new Address(bytes);
        this.currencyType = currencyType;
    }

    @Override
    public String toString() {
        return address.toString();
    }

    @NotNull
    @Override
    public HdKeyPath getBip32Path() {
        return address.getBip32Path();
    }

    @NotNull
    @Override
    public Address getAddress() {
        return address;
    }

    @NotNull
    @Override
    public CryptoCurrency getCoinType() {
        return currencyType;
    }

    @NotNull
    @Override
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

    @Override
    public long getId() {
        return 0;
    }

    @NotNull
    @Override
    public byte[] getBytes() {
        return address.getAllAddressBytes();
    }
}
