package com.mycelium.wapi.wallet;

public enum BroadcastResultType {
    SUCCESS,
    REJECTED,
    NO_SERVER_CONNECTION,

    REJECT_MALFORMED,
    REJECT_DUPLICATE,
    REJECT_NONSTANDARD,
    REJECT_INSUFFICIENT_FEE,
    REJECT_INVALID_TX_PARAMS,

    /**
     * The connection dropped after the transaction had been sent, and the node could not be
     * asked afterwards whether it got it: the transaction may or may not be on the network.
     * Must not be presented as a failure (the user would re-send and pay twice).
     */
    UNKNOWN_OUTCOME
}
