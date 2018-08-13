package com.mycelium.wallet.event;

import android.os.Handler;

public class RetrieveTransactionSummaryFailed {
    public final Handler handler;

    public RetrieveTransactionSummaryFailed(Handler handler){
        this.handler = handler;
    }
}
