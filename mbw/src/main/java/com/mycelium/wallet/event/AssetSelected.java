package com.mycelium.wallet.event;

import com.mycelium.wapi.wallet.Address;

public class AssetSelected {
    public final Address address;

    public AssetSelected(Address address){
        this.address = address;
    }
}
