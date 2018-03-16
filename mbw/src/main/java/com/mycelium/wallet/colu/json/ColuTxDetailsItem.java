package com.mycelium.wallet.colu.json;

import com.mrd.bitlib.model.Address;
import com.mycelium.wapi.model.TransactionDetails;

import java.math.BigDecimal;

public class ColuTxDetailsItem extends TransactionDetails.Item {
    private final long assetAmount;

    private final int scale;

    public ColuTxDetailsItem(Address address, long value, boolean isCoinbase, long assetAmount, int scale) {
        super(address, value, isCoinbase);
        this.assetAmount = assetAmount;
        this.scale = scale;
    }

    public BigDecimal getAmount() {
        return BigDecimal.valueOf(assetAmount, scale);
    }
}
