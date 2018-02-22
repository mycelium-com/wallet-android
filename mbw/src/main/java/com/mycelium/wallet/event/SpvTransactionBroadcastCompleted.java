package com.mycelium.wallet.event;

/**
 * Created by itserg on 22.02.18.
 */

public class SpvTransactionBroadcastCompleted {
    public final String operationId;
    public final String txHash;

    public SpvTransactionBroadcastCompleted(String operationId, String txHash) {
        this.operationId = operationId;
        this.txHash = txHash;
    }
}
