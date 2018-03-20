package com.mycelium.wallet.event;

import com.mycelium.modularizationtools.model.Module;

import java.util.Date;

public class SpvSyncChanged {
    public final Module module;
    public final Date bestChainDate;
    public final long blockHeight;
    public final float chainDownloadPercentDone;

    public SpvSyncChanged(Module module, Date bestChainDate, long blockHeight, float chainDownloadPercentDone) {
        this.module = module;
        this.bestChainDate = bestChainDate;
        this.blockHeight = blockHeight;
        this.chainDownloadPercentDone = chainDownloadPercentDone;
    }
}
