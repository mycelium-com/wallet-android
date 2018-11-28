package com.mycelium.wallet.event;

import com.mycelium.wapi.wallet.GenericAddress;

public class AssetSelected {
    public final GenericAddress address;

    public AssetSelected(GenericAddress address){
        this.address = address;
    }
}
