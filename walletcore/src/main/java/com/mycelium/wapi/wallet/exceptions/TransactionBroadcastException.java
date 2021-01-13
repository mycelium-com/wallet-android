package com.mycelium.wapi.wallet.exceptions;

public class TransactionBroadcastException extends Exception {
    public TransactionBroadcastException(String message) {
        super(message);
    }

    public TransactionBroadcastException(Throwable cause) {
        super(cause);
    }
}
