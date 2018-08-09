package com.mycelium.wapi.wallet.exceptions;

public class AddressMalformedException extends Exception {
    public AddressMalformedException(String message) {
        super(message);
    }

    public AddressMalformedException(Throwable cause) {
        super(cause);
    }
}
