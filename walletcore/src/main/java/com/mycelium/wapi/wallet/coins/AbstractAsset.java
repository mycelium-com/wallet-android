package com.mycelium.wapi.wallet.coins;

import com.mycelium.wapi.wallet.coins.families.Families;

public abstract class AbstractAsset implements GenericAssetInfo {
    protected Families family;

    protected String name;
    protected String symbol;

    protected transient Value oneCoin;
}
