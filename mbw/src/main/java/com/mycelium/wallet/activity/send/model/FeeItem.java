package com.mycelium.wallet.activity.send.model;

/**
 * Created by elvis on 31.08.17.
 */

public class FeeItem {
    public int type;
    public long feePerKb;

    public FeeItem(long feePerKb, int type) {
        this.type = type;
        this.feePerKb = feePerKb;
    }
}
