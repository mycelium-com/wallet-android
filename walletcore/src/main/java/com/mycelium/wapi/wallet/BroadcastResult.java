package com.mycelium.wapi.wallet;

public class BroadcastResult {
    private String errorMessage = null;
    private final BroadcastResultType resultType;

    public BroadcastResult(BroadcastResultType resultType){
        this.resultType = resultType;
    }
    public BroadcastResult(String errorMessage, BroadcastResultType resultType){
        this.errorMessage = errorMessage;
        this.resultType = resultType;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public BroadcastResultType getResultType() {
        return resultType;
    }
}
