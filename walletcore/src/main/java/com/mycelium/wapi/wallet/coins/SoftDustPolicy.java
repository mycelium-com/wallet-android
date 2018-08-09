package com.mycelium.wapi.wallet.coins;

public enum SoftDustPolicy {
    // Used by coins like Litecoin and Dogecoin, where if a soft dust TXOs are present, add base fee
    // for each soft dust TXO additionally to normal fees
    BASE_FEE_FOR_EACH_SOFT_DUST_TXO,
    // Used in Bitcoin, Peercoin and NuBits, where if a dust TXO is present make the fee at least base fee.
    // For example if a transaction qualify as free and it has a soft dust TXO, then this tx must
    // pay the base fee.
    AT_LEAST_BASE_FEE_IF_SOFT_DUST_TXO_PRESENT,
    // Used in coins that don't have a soft dust detection
    NO_POLICY,
}