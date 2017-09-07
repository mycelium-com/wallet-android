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
}
