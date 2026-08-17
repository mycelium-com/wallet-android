package com.mycelium.wapi.wallet.tron

/**
 * Classification of a TRON transaction by the on-chain contract type it carries.
 *
 * Historically the wallet only stored plain TRX transfers (TransferContract) and TRC20
 * `Transfer` events, dropping every other contract type before it reached the DB. This enum
 * lets us surface the full, wallet-relevant set of TRON operations (staking, resource
 * delegation, voting, reward claiming, contract calls, …) in the transaction history.
 *
 * The DB stores the enum [name]; [fromContractType] maps the raw TRON contract-type string
 * (as returned by the node in `raw_data.contract[].type`) into one of these buckets.
 */
enum class TronTxType(val contractTypes: Set<String>) {
    /** Native TRX transfer. */
    TRX_TRANSFER(setOf("TransferContract")),

    // TRC-10 asset transfer — intentionally disabled: the wallet doesn't support TRC10 assets,
    // and incoming TransferAssetContract txs are almost always unsolicited scam/airdrop tokens,
    // so they are excluded from history entirely (see TRC10_CONTRACT_TYPE filter in
    // TronAccountBacking.putTransactions). Uncomment to surface them again.
    // TRC10_TRANSFER(setOf("TransferAssetContract")),

    /** TRC-20 token transfer (synthetic — TRC20 events come from a dedicated endpoint; see TRC20_MARKER). */
    TRC20_TRANSFER(setOf("__Trc20Transfer__")),

    /** Generic smart-contract interaction / deployment (swaps, approvals, dApps …). */
    CONTRACT_CALL(setOf("TriggerSmartContract", "CreateSmartContract")),

    /** Stake TRX for Bandwidth/Energy (Stake 2.0 FreezeBalanceV2, and legacy FreezeBalance). */
    STAKE(setOf("FreezeBalanceV2Contract", "FreezeBalanceContract")),

    /** Unstake TRX (Stake 2.0 UnfreezeBalanceV2, and legacy UnfreezeBalance). */
    UNSTAKE(setOf("UnfreezeBalanceV2Contract", "UnfreezeBalanceContract")),

    /** Withdraw TRX after the 14-day unstaking period completes. */
    WITHDRAW_UNFROZEN(setOf("WithdrawExpireUnfreezeContract")),

    /** Cancel all pending (not-yet-withdrawable) unstaking operations. */
    CANCEL_UNSTAKE(setOf("CancelAllUnfreezeV2Contract")),

    /** Delegate Bandwidth/Energy resource to another account. */
    DELEGATE_RESOURCE(setOf("DelegateResourceContract")),

    /** Reclaim previously delegated Bandwidth/Energy resource. */
    UNDELEGATE_RESOURCE(setOf("UnDelegateResourceContract")),

    /** Vote for Super Representatives. */
    VOTE(setOf("VoteWitnessContract")),

    /** Claim staking/voting rewards (WithdrawBalance). */
    CLAIM_REWARDS(setOf("WithdrawBalanceContract")),

    /** On-chain account activation. */
    ACCOUNT_ACTIVATION(setOf("AccountCreateContract")),

    /**
     * Account permission change (owner/active keys). Legitimate only when the user set it up
     * themselves (e.g. the wallet's staking grant); otherwise the signature move of the
     * "multi-sig injection" scam — rendered with warning styling in the history.
     */
    PERMISSION_UPDATE(setOf("AccountPermissionUpdateContract")),

    /** Any other / unrecognised contract type. */
    UNKNOWN(emptySet());

    /** True for plain value transfers (TRX / TRC20), which render with the classic send/receive UI. */
    val isTransfer: Boolean
        get() = this == TRX_TRANSFER || /* this == TRC10_TRANSFER || */ this == TRC20_TRANSFER

    companion object {
        /** Synthetic contract-type marker for TRC20 transfers (which don't come from raw_data.contract). */
        const val TRC20_MARKER = "__Trc20Transfer__"

        /**
         * Raw contract type of TRC10 transfers. Kept as a named constant so persistence can drop
         * these txs before they reach the DB (TRC10 is unsupported / scam-airdrop noise).
         */
        const val TRC10_CONTRACT_TYPE = "TransferAssetContract"

        /** Maps a raw TRON contract-type string to a [TronTxType]; unknown/absent → [UNKNOWN]. */
        fun fromContractType(contractType: String?): TronTxType =
            entries.firstOrNull { contractType != null && contractType in it.contractTypes } ?: UNKNOWN

        /** Parses a stored enum name back to [TronTxType], tolerating legacy/absent values. */
        fun fromName(name: String?): TronTxType = when {
            // Rows persisted while TRC10 was still classified must not fall back to TRX_TRANSFER
            // (their value is in TRC10 asset units, not sun).
            name == "TRC10_TRANSFER" -> UNKNOWN
            else -> entries.firstOrNull { it.name == name } ?: TRX_TRANSFER
        }
    }
}
