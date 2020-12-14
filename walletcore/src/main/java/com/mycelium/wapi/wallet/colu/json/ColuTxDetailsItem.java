package com.mycelium.wapi.wallet.colu.json;

import com.mrd.bitlib.model.BitcoinAddress;
import com.mycelium.wapi.model.TransactionDetails;

import java.math.BigDecimal;

public class ColuTxDetailsItem extends TransactionDetails.Item {
    private final long assetAmount;

    private final int scale;

    public ColuTxDetailsItem(BitcoinAddress address, long value, boolean isCoinbase, long assetAmount, int scale) {
        super(address, value, isCoinbase);
        this.assetAmount = assetAmount;
        this.scale = scale;
    }

    public BigDecimal getAmount() {
        return BigDecimal.valueOf(assetAmount, scale);
    }
}
