package com.mycelium.wallet.simplex;

import android.os.Handler;

public class SimplexError {
    public final Handler handler;
    public final String message;

    public SimplexError(Handler handler, String message) {
        this.handler = handler;
        this.message = message;
    }
}
