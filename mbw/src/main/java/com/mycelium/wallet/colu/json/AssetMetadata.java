package com.mycelium.wallet.colu.json;

import com.google.api.client.util.Key;

import java.math.BigDecimal;

public class AssetMetadata {
    @Key
    public String assetId;

    @Key
    public int divisibility;

    @Key
    public long totalSupply;

    public AssetMetadata(String assetId, BigDecimal value) {
        this.assetId = assetId;
        this.divisibility = value.scale();
        this.totalSupply = value.movePointRight(this.divisibility).longValue();
    }

    public BigDecimal getTotalSupply() {
        return BigDecimal.valueOf(totalSupply).movePointLeft(divisibility);
    }
}
