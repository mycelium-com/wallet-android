package com.mycelium.wallet.activity.send.model;

import com.mycelium.wapi.wallet.coins.Value;

public class FeeItem {
    public long feePerKb;
    public Value value; // Fee value in minimal asset's units
    public Value fiatValue;
    /** as defined in {@link com.mycelium.wallet.activity.send.view.SelectableRecyclerView.Adapter} */
    public int type;

    public FeeItem(long feePerKb, Value value, Value fiatValue, int type) {
        this.feePerKb = feePerKb;
        this.value = value;
        this.fiatValue = fiatValue;
        this.type = type;
    }

    public FeeItem(int type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FeeItem feeItem = (FeeItem) o;

        if (feePerKb != feeItem.feePerKb) return false;
        return type == feeItem.type;
    }

    @Override
    public int hashCode() {
        int result = (int) (feePerKb ^ (feePerKb >>> 32));
        result = 31 * result + type;
        return result;
    }
}
