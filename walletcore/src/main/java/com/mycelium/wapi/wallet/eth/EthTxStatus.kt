package com.mycelium.wapi.wallet.eth

/**
 * Lifecycle state of an ETH/ERC20 transaction as tracked locally.
 *
 * Source-of-truth column in EthAccountBacking. Values are stored as TEXT
 * (the enum name) so that adding new states later does not break the
 * schema.
 *
 * Distinctions worth keeping in mind:
 * - PENDING: live in mempool; included by the per-tx blockbook endpoint
 *   even when the address-level index temporarily forgets it.
 * - CONFIRMED: mined and on-chain `success` flag is true.
 * - FAILED: mined but on-chain `success` flag is false (revert).
 * - REPLACED: another confirmed tx with the same `from + nonce` exists,
 *   meaning this one was bumped out (RBF / cancel pattern).
 * - CANCELED: a REPLACED tx whose replacement is a 0-value self-transfer,
 *   the canonical "cancel" RBF.
 * - DROPPED: not known to per-tx endpoint, no replacement seen, and the
 *   PENDING-grace timeout has elapsed. Truly fell out of mempools.
 */
enum class EthTxStatus {
    PENDING,
    CONFIRMED,
    FAILED,
    REPLACED,
    CANCELED,
    DROPPED;

    val isTerminal: Boolean
        get() = this != PENDING

    val isAlive: Boolean
        get() = this == PENDING || this == CONFIRMED

    companion object {
        fun parse(name: String?): EthTxStatus = when (name) {
            null -> PENDING
            else -> entries.firstOrNull { it.name == name } ?: PENDING
        }
    }
}
