package com.mycelium.wapi.wallet.btc;

import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.AddressType;
import com.mycelium.wapi.wallet.GenericAddress;
import com.mycelium.wapi.wallet.coins.CryptoCurrency;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class BtcAddress implements GenericAddress {

    private Address address;
    private CryptoCurrency currencyType;

    public BtcAddress(CryptoCurrency currencyType, Address address) {
        this.address = address;
        this.currencyType = currencyType;
    }

    @Override
    public String toString() {
        return address.toString();
    }

    @NotNull
    public Address getAddress() {
        return address;
    }

    @NotNull
    @Override
    public CryptoCurrency getCoinType() {
        return currencyType;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BtcAddress that = (BtcAddress) o;
        return Objects.equals(address, that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address);
    }

    @NotNull
    @Override
    public String getSubType() {
        return address.getType().name();
    }

    public AddressType getType()
    {
        return address.getType();
    }

}
