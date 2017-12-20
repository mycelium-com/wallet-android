package com.mycelium.wallet.event;

import java.util.Date;

public class SpvSyncChanged {
    public final Date bestChainDate;
    public final long blockHeight;

    public SpvSyncChanged(Date bestChainDate, long blockHeight) {
        this.bestChainDate = bestChainDate;
        this.blockHeight = blockHeight;
    }
}
