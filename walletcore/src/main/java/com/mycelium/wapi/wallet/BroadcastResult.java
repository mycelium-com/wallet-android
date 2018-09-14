package com.mycelium.wapi.wallet;

public enum BroadcastResult {
    SUCCESS,
    REJECTED,
    REJECTED_DOUBLE_SPENDING,
    NO_SERVER_CONNECTION
}
