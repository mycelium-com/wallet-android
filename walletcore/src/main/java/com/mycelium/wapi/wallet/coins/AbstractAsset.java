package com.mycelium.wapi.wallet.coins;

import com.mycelium.wapi.wallet.MonetaryFormat;
import com.mycelium.wapi.wallet.coins.families.Families;

public abstract class AbstractAsset implements GenericAssetInfo {
    protected Families family;

    protected String name;
    protected String symbol;

    protected transient MonetaryFormat friendlyFormat;
    protected transient MonetaryFormat plainFormat;
    protected transient Value oneCoin;
}
