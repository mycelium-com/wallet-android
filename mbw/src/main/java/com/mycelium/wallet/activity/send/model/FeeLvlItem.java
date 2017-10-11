package com.mycelium.wallet.activity.send.model;

import com.mycelium.wallet.MinerFee;

/**
 * Created by elvis on 31.08.17.
 */

public class FeeLvlItem {
    public MinerFee minerFee;
    public String duration;
    public int type;

    public FeeLvlItem(MinerFee minerFee, String duration, int type) {
        this.minerFee = minerFee;
        this.duration = duration;
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FeeLvlItem that = (FeeLvlItem) o;

        if (type != that.type) return false;
        return minerFee == that.minerFee;

    }

    @Override
    public int hashCode() {
        int result = minerFee != null ? minerFee.hashCode() : 0;
        result = 31 * result + type;
        return result;
    }
}
