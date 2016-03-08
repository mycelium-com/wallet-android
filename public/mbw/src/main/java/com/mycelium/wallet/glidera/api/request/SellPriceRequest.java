package com.mycelium.wallet.glidera.api.request;

import com.google.api.client.repackaged.com.google.common.base.Preconditions;

import java.math.BigDecimal;

public class SellPriceRequest {
    private final BigDecimal qty;
    private final BigDecimal fiat;

    /**
     * @param qty  Amount to purchase in Bitcoin (ex. 1.2). Either qty or fiat is required
     * @param fiat Amount to purchase in USD (ex. 1.25). Either qty or fiat is required
     */
    public SellPriceRequest(BigDecimal qty, BigDecimal fiat) {
        Preconditions.checkArgument(qty == null ^ fiat == null);
        this.qty = qty;
        this.fiat = fiat;
    }

    public BigDecimal getQty() {
        return qty;
    }

    public BigDecimal getFiat() {
        return fiat;
    }
}
