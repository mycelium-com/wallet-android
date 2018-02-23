package com.mycelium.wallet.event;

/**
 * Created by itserg on 22.02.18.
 */

public class SpvSendFundsResult {
    public final String operationId;
    public final boolean isSuccess;
    public final String message;
    public final String txHash;

    public SpvSendFundsResult(String operationId, String txHash, boolean isSuccess, String message) {
        this.operationId = operationId;
        this.txHash = txHash;
        this.isSuccess = isSuccess;
        this.message = message;
    }
}
