package com.mrd.bitlib.crypto.schnorr;


public class UnauthorisedException extends Exception {
    private static final long serialVersionUID = 1L;

    public UnauthorisedException(String message) {
        super(message);
    }
}