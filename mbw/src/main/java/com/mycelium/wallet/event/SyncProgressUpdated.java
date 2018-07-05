package com.mycelium.wallet.event;

import java.util.UUID;

public class SyncProgressUpdated {
    public final UUID account;

    public SyncProgressUpdated(UUID account) {
        this.account = account;
    }
}
