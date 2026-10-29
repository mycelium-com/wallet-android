package com.mycelium.wapi.wallet.eth.aa

import org.web3j.utils.Numeric
import java.math.BigInteger

/** One call of a batched UserOperation (e.g. approve + transfer). */
data class Call(
    val to: String,
    val value: BigInteger = BigInteger.ZERO,
    val data: ByteArray = ByteArray(0)
)

/** ERC-4337 UserOperation (v0.7/v0.8 unpacked RPC format). */
data class UserOperation(
    val sender: String,
    val nonce: BigInteger,
    val callData: ByteArray,
    val callGasLimit: BigInteger = BigInteger.ZERO,
    val verificationGasLimit: BigInteger = BigInteger.ZERO,
    val preVerificationGas: BigInteger = BigInteger.ZERO,
    val maxFeePerGas: BigInteger = BigInteger.ZERO,
    val maxPriorityFeePerGas: BigInteger = BigInteger.ZERO,
    val paymaster: String? = null,
    val paymasterVerificationGasLimit: BigInteger? = null,
    val paymasterPostOpGasLimit: BigInteger? = null,
    val paymasterData: ByteArray? = null,
    val signature: ByteArray = DUMMY_SIGNATURE
) {
    fun totalGasLimit(): BigInteger = preVerificationGas + verificationGasLimit + callGasLimit +
            (paymasterVerificationGasLimit ?: BigInteger.ZERO) + (paymasterPostOpGasLimit ?: BigInteger.ZERO)

    fun maxCostWei(): BigInteger = totalGasLimit() * maxFeePerGas

    fun withPaymaster(paymaster: String, paymasterData: ByteArray,
                      verificationGasLimit: BigInteger, postOpGasLimit: BigInteger) = copy(
        paymaster = paymaster,
        paymasterData = paymasterData,
        paymasterVerificationGasLimit = verificationGasLimit,
        paymasterPostOpGasLimit = postOpGasLimit
    )

    companion object {
        /**
         * Dummy ECDSA signature for gas estimation: Simple7702Account runs ECDSA.recover in
         * validateUserOp, which reverts on garbage, so r and s are valid secp256k1 scalars.
         * Same value permissionless.js uses.
         */
        val DUMMY_SIGNATURE: ByteArray = Numeric.hexStringToByteArray(
            "fffffffffffffffffffffffffffffff0000000000000000000000000000000007aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1c"
        )
    }
}

/** Signed EIP-7702 authorization tuple, hex fields as the bundler expects them. */
data class Authorization7702(
    val chainId: String,
    val address: String,
    val nonce: String,
    val yParity: String,
    val r: String,
    val s: String
)

/** State override for bundler simulation (EIP-7702: code = 0xef0100 || delegate). */
data class StateOverride(
    val code: String? = null,
    val balance: String? = null
)

data class GasPrice(val maxFeePerGas: BigInteger, val maxPriorityFeePerGas: BigInteger)

data class GasEstimate(
    val callGasLimit: BigInteger,
    val verificationGasLimit: BigInteger,
    val preVerificationGas: BigInteger
)

data class PaymasterData(
    val paymaster: String,
    val paymasterData: ByteArray,
    val paymasterVerificationGasLimit: BigInteger?,
    val paymasterPostOpGasLimit: BigInteger?
)

data class ReceiptLog(val address: String, val topics: List<String>, val data: String)

data class UserOpReceipt(
    val userOpHash: String,
    val transactionHash: String,
    val success: Boolean,
    val actualGasCost: BigInteger,
    val blockNumber: Long = 0,
    val logs: List<ReceiptLog> = emptyList()
) {
    /** Sum of [token] Transfer events out of [owner] in this op: the amount plus the fee. */
    fun tokenDebited(token: String, owner: String): BigInteger = logs
        .filter {
            it.address.equals(token, ignoreCase = true) && it.topics.size >= 3 &&
                    it.topics[0].equals(TRANSFER_TOPIC, ignoreCase = true) &&
                    it.topics[1].takeLast(40).equals(Numeric.cleanHexPrefix(owner), ignoreCase = true)
        }
        .fold(BigInteger.ZERO) { sum, log -> sum + AARpcClient.hexToBig(log.data) }

    companion object {
        /** keccak256("Transfer(address,address,uint256)") */
        const val TRANSFER_TOPIC = "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef"
    }
}

/** Fee for one UserOperation, paid in an ERC-20 token. */
data class FeeQuote(
    val tokenAddress: String,
    val tokenSymbol: String,
    val tokenDecimals: Int,
    /** Expected fee, token smallest units — what the user is shown. */
    val tokenAmount: BigInteger,
    /** Upper bound (estimate + buffer) — what is approved and has to stay in the account. */
    val maxTokenAmount: BigInteger,
    /** Token units per ETH, scaled by 1e18. */
    val exchangeRate: BigInteger,
    val gasCostWei: BigInteger,
    val paymasterAddress: String,
    val paymasterData: ByteArray,
    val paymasterVerificationGasLimit: BigInteger,
    val paymasterPostOpGasLimit: BigInteger,
    /** UserOperation gas captured during estimation; reused on send. */
    val callGasLimit: BigInteger,
    val verificationGasLimit: BigInteger,
    val preVerificationGas: BigInteger,
    val maxFeePerGas: BigInteger,
    val maxPriorityFeePerGas: BigInteger,
    /** Validity window (epoch seconds, 0 = not set). */
    val validUntil: Long,
    val validAfter: Long,
    val createdAtMs: Long = System.currentTimeMillis()
)

sealed class AATransactionResult {
    /** [tokenDebited]: what left the sender in the fee token — the amount plus the fee actually charged. */
    data class Success(val userOpHash: String, val transactionHash: String, val blockNumber: Long,
                       val tokenDebited: BigInteger) : AATransactionResult()
    data class Failure(val error: AAError, val message: String) : AATransactionResult()
}

enum class AAError {
    INSUFFICIENT_TOKEN_BALANCE,
    PAYMASTER_REJECTED,
    BUNDLER_REJECTED,
    USER_OP_REVERTED,
    GAS_ESTIMATION_FAILED,
    FEE_QUOTE_EXPIRED,
    /**
     * The paymaster asked for something the wallet will not sign: an approval far above the fee
     * the user was shown, or a paymaster other than the configured one.
     */
    FEE_QUOTE_REJECTED,
    /** An ETH-paid tx of the sender is pending; the EIP-7702 authorization would conflict with it. */
    PENDING_EOA_TX,
    TIMEOUT,
    NETWORK_ERROR,
    UNKNOWN
}

class AARpcException(message: String, val code: Int? = null) : Exception(message)
