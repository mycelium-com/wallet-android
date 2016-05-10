package com.mycelium.wallet.external.glidera.api;

public class Nonce {
    private Long nonce;

    public synchronized long getNonce() {
        if (nonce == null) {
            resetNonce();
        } else {
            nonce++;
        }

        return nonce;
    }

    public synchronized void resetNonce() {
        nonce = System.currentTimeMillis();
    }

    public synchronized String getNonceString() {
        return String.valueOf(getNonce());
    }
}
