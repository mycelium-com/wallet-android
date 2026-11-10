package com.mycelium.wapi.wallet.tron.energy

import java.math.BigInteger

/** Where the partner API lives; keys differ per network / build type. */
data class EnergyServiceConfig(val baseUrl: String, val apiKey: String) {
    val isConfigured: Boolean get() = baseUrl.isNotBlank() && apiKey.isNotBlank()
}

/** Non-2xx answer from the partner API; `detail.error` / `detail.message` when present. */
class EnergyServiceException(
    val statusCode: Int,
    override val message: String,
    val errorCode: String? = null,
) : Exception(message) {
    val isInsufficientBalance: Boolean get() = errorCode == "INSUFFICIENT_BALANCE"
    val isNotFound: Boolean get() = statusCode == 404
}

data class EnergyServiceHealth(
    val status: String,
    val networkMode: String,
    val balanceEnergy: Boolean,
    val balanceBandwidth: Boolean,
    /** Contract addresses (base58) of the tokens the partner sponsors. */
    val supportedTokens: List<String>,
) {
    fun supports(contractAddress: String) =
        status == "healthy" && balanceEnergy && balanceBandwidth &&
                supportedTokens.any { it.equals(contractAddress, ignoreCase = true) }
}

/**
 * Fee quote from `GET api/v1/fee-info`. [feeAmountUnits] is the authoritative integer fee in
 * [tokenDecimals] units (the decimal string [feeAmount] is only for display).
 */
data class EnergyFeeQuote(
    val transactionId: Int,
    val feeAmount: String,
    val feeAmountUnits: BigInteger,
    val tokenSymbol: String,
    val tokenContract: String,
    val tokenDecimals: Int,
    val transferMethod: String,
    /** Recipient of the fee payment (two_transactions method). */
    val feeWallet: String?,
    val energyNeeded: Long,
    val bandwidthNeeded: Long,
    val recipientHasToken: Boolean,
    val minFeeLimit: Long,
    /** Local epoch ms after which the partner will reject the quote. */
    val expiresAtMs: Long,
    val estimatedTimeSec: Int?,
    val balanceOk: Boolean,
    val balanceMessage: String?,
) {
    fun isExpired(nowMs: Long = System.currentTimeMillis()) = nowMs >= expiresAtMs

    /**
     * Fee in the wallet's token units (the wallet's [walletDecimals] may differ from what the
     * backend reports for the same token — rescale the integer, never go through doubles).
     */
    fun feeInWalletUnits(walletDecimals: Int): BigInteger = when {
        walletDecimals > tokenDecimals -> feeAmountUnits * BigInteger.TEN.pow(walletDecimals - tokenDecimals)
        walletDecimals < tokenDecimals -> feeAmountUnits / BigInteger.TEN.pow(tokenDecimals - walletDecimals)
        else -> feeAmountUnits
    }
}

/** `POST api/v1/transfer/execute` answer — the order is always processed asynchronously. */
data class EnergyExecuteResponse(val success: Boolean, val orderId: String, val status: String, val message: String)

data class EnergyOrderStatus(
    val orderId: String,
    val status: String,
    /** Main transfer hash once the partner broadcast it. */
    val txHash: String?,
    /** Fee payment hash once broadcast. */
    val feeTxHash: String?,
    val errorCode: String?,
    val errorMessage: String?,
) {
    val isTerminal: Boolean get() = status in TERMINAL
    val isSuccess: Boolean get() = status == "confirmed"

    companion object {
        val TERMINAL = setOf("confirmed", "failed", "partially_failed")
    }
}

/** Outcome of an energy-service transfer as seen by the wallet. */
sealed class EnergyTransferResult {
    /** Both txs confirmed by the partner. */
    data class Success(val txHash: String, val feeTxHash: String?) : EnergyTransferResult()
    /** Order accepted but not terminal within the polling window — do NOT resend. */
    data class Pending(val orderId: String) : EnergyTransferResult()
    data class Error(val message: String, val cause: Exception? = null) : EnergyTransferResult()
    object QuoteExpired : EnergyTransferResult()
    object InsufficientBalance : EnergyTransferResult()
}

/** Marker of an order the wallet handed to the partner and has not seen terminal yet. */
data class EnergyOrderInfo(
    val orderId: String,
    val accountId: java.util.UUID,
    val createdAtMs: Long,
    val mainTxId: String?,
    /** `raw_data.expiration` of the signed main tx (ms); after it the partner cannot broadcast anymore. */
    val mainTxExpirationMs: Long?,
)
