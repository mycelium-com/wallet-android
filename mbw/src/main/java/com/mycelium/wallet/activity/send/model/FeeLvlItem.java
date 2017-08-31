package com.mycelium.wallet.activity.send.model;

import com.mycelium.wallet.MinerFee;

/**
 * Created by elvis on 31.08.17.
 */

public class FeeLvlItem {
    public int type;
    public MinerFee minerFee;

    public FeeLvlItem(MinerFee minerFee, int type) {
        this.type = type;
        this.minerFee = minerFee;
    }
}
