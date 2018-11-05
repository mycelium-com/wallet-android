package com.mycelium.wapi.wallet.coins;

import com.mycelium.wapi.wallet.coins.families.Families;

public abstract class AbstractAsset implements GenericAssetInfo {
    protected Families family;

    protected String name;
    protected String symbol;

    protected transient Value oneCoin;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null)
            return false;
        if (getClass() != o.getClass())
            return false;
        AbstractAsset other = (AbstractAsset) o;
        return getId().equals(other.getId());
    }
}
