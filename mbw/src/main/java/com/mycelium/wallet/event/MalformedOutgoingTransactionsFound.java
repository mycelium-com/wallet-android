package com.mycelium.wallet.event;

import java.util.UUID;

public class MalformedOutgoingTransactionsFound {
    public final UUID account;

    public MalformedOutgoingTransactionsFound(UUID account) {
        this.account =   account;
    }
}

