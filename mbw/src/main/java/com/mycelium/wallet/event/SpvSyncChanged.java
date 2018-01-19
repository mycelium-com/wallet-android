package com.mycelium.wallet.event;

import java.util.Date;

public class SpvSyncChanged {
    public final Date bestChainDate;
    public final long blockHeight;
    public final int chainDownloadPercentDone;

    public SpvSyncChanged(Date bestChainDate, long blockHeight, int chainDownloadPercentDone) {
        this.bestChainDate = bestChainDate;
        this.blockHeight = blockHeight;
        this.chainDownloadPercentDone = chainDownloadPercentDone;
    }
}
