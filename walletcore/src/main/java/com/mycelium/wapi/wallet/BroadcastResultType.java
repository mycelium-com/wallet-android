package com.mycelium.wapi.wallet;

public enum BroadcastResultType {
    SUCCESS,
    REJECTED,
    NO_SERVER_CONNECTION,

    REJECT_MALFORMED,
    REJECT_DUPLICATE,
    REJECT_NONSTANDARD,
    REJECT_INSUFFICIENT_FEE
}
