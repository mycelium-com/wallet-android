package com.mycelium.wallet.colu.json;

import com.mrd.bitlib.model.Address;
import com.mycelium.wapi.model.TransactionDetails;

/**
 * Created by kot on 09.07.17.
 */

public class ColuTxDetailsItem extends TransactionDetails.Item {
    public final long assetAmount;

    public final int scale;

    public ColuTxDetailsItem(Address address, long value, boolean isCoinbase, long assetAmount, int scale) {
        super(address, value, isCoinbase);
        this.assetAmount = assetAmount;
        this.scale = scale;
    }
}
