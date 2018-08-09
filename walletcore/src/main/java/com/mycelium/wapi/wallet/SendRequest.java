package com.mycelium.wapi.wallet;

public class SendRequest<T extends AbstractTransaction> {

    public T tx;

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }


    // Tracks if this has been passed to wallet.completeTransaction already: just a safety check.
    private boolean completed;
}
