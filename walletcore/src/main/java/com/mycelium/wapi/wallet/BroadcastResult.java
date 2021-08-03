package com.mycelium.wapi.wallet;

import javax.annotation.Nullable;

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

    @Nullable
    public String getErrorMessage() {
        return errorMessage;
    }

    public BroadcastResultType getResultType() {
        return resultType;
    }
}
