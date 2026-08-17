package com.mycelium.wapi.wallet.tron.remote

import java.math.BigInteger

/** TronGrid-compatible HTTP endpoint (TronGrid itself, or a java-tron full node HTTP API). */
data class TronEndpoint(
    val baseUrl: String,
    /** TronGrid `TRON-PRO-API-KEY`; null for keyless access (throttled to ~1 QPS by TronGrid). */
    val apiKey: String? = null,
) {
    /** Requests per second this endpoint tolerates: TronGrid allows 15 with a key, 1 without. */
    val allowedRps: Double
        get() = if (apiKey.isNullOrBlank()) 1.0 else 12.0
}

interface ServerTronListChangedListener {
    fun serverListChanged(newEndpoints: List<TronEndpoint>)
}

data class TronBalanceResponse(
    /** Spendable TRX in sun. */
    val confirmed: BigInteger,
    /** False when the account has never been activated on-chain (TronGrid returns no data). */
    val exists: Boolean,
    /** TRX (sun) frozen for staking (Stake 2.0), informational. */
    val stakedSun: BigInteger = BigInteger.ZERO,
)

/**
 * A TRON transaction as surfaced by TronGrid `v1/accounts/{address}/transactions`, normalized:
 * base58 addresses, sun amounts, and the raw contract type for classification.
 */
data class TronTx(
    val txid: String,
    val blockNumber: Long,
    /** Block timestamp in milliseconds. */
    val blockTimestampMs: Long,
    val from: String,
    /** Recipient (transfers) / counterparty; "" when the contract type has none. */
    val to: String,
    /** Principal amount in sun (transfer amount, staked amount, …). */
    val value: BigInteger,
    /** TRX burned for the tx in sun (bandwidth + energy). */
    val fee: BigInteger,
    val success: Boolean,
    /** Raw `raw_data.contract[0].type`, e.g. "TransferContract". */
    val contractType: String?,
    val energyUsage: Long = 0,
    val netUsage: Long = 0,
) {
    val blockTimestampSec: Long get() = blockTimestampMs / 1000
}

/** A TRC20 `Transfer` event from TronGrid `v1/accounts/{address}/transactions/trc20`. */
data class TronTrc20Transfer(
    val txid: String,
    val blockTimestampMs: Long,
    val from: String,
    val to: String,
    val value: BigInteger,
    val contractAddress: String,
    val symbol: String,
    val decimals: Int,
    /** "Transfer" / "Approval". */
    val type: String,
)

data class TronTransactionInfo(
    val id: String?,
    /** 0 when the node does not know the tx (yet). */
    val blockNumber: Long,
    val blockTimestampMs: Long,
    /** Total fee charged in sun. */
    val fee: Long,
    /** `receipt.result` for smart-contract txs (SUCCESS / OUT_OF_ENERGY / REVERT …); null for plain transfers. */
    val receiptResult: String?,
    val energyUsageTotal: Long,
    val netUsage: Long,
    val netFee: Long,
    val energyFee: Long,
) {
    val isConfirmed: Boolean get() = blockNumber > 0
    val isSuccess: Boolean get() = receiptResult == null || receiptResult == "SUCCESS"
}

data class AccountResourceInfo(
    val freeNetAvailable: Long,
    val netAvailable: Long,
    val energyAvailable: Long,
    val totalNetLimit: Long,
    val totalEnergyLimit: Long,
) {
    val bandwidthAvailable: Long get() = freeNetAvailable + netAvailable

    companion object {
        val EMPTY = AccountResourceInfo(0, 0, 0, 0, 0)
    }
}

data class TriggerConstantResult(
    val success: Boolean,
    val message: String?,
    val energyUsed: Long,
    val constantResult: List<String>,
    /** `transaction.ret[0].contractRet` of the simulated tx, when present. */
    val contractRet: String?,
)

data class TRC20TokenInfo(
    val contractAddress: String,
    val name: String,
    val symbol: String,
    val decimals: Int,
)

data class SendResult(val success: Boolean, val txid: String?, val message: String?, val code: String? = null)
