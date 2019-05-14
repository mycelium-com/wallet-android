package com.mycelium.wapi.wallet.exceptions;

public class GenericTransactionBroadcastException extends Exception {
    public GenericTransactionBroadcastException(String message) {
        super(message);
    }

    public GenericTransactionBroadcastException(Throwable cause) {
        super(cause);
    }
}
