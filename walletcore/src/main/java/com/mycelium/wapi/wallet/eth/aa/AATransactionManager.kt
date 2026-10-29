package com.mycelium.wapi.wallet.eth.aa

import com.fasterxml.jackson.databind.JsonNode
import org.web3j.crypto.Hash
import org.web3j.crypto.Sign
import org.web3j.rlp.RlpEncoder
import org.web3j.rlp.RlpList
import org.web3j.rlp.RlpString
import org.web3j.utils.Numeric
import java.io.IOException
import java.math.BigInteger
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import java.util.logging.Logger

/** Signs a raw 32-byte hash with the account key (no message prefix). */
typealias HashSigner = (ByteArray) -> Sign.SignatureData

/**
 * Pays the gas of an ERC-20 transfer in the token itself (ERC-4337 + EIP-7702 + ERC-20
 * paymaster, Pimlico by default). The EOA delegates to Simple7702Account with an EIP-7702
 * authorization, and a UserOperation runs `[approve(paymaster), transfer(recipient)]` in one
 * batch; the paymaster pays the ETH gas and takes its cost in the token in postOp.
 *
 * Port of WalletKMP's `AATransactionManager` + `FeeEstimator` + `PimlicoPaymasterService` +
 * `DelegationManager`. All calls are blocking — run them off the main thread.
 */
class AATransactionManager @JvmOverloads constructor(
    val config: ChainAAConfig,
    private val rpc: AARpcClient = AARpcClient(config)
) {
    private val logger = Logger.getLogger(AATransactionManager::class.simpleName)

    /**
     * What this process itself already did on-chain, per sender (lowercase). The node is load
     * balanced: right after a receipt the next read can come from a node a block behind, which
     * for a second send in a row means the old EntryPoint nonce (AA25) or "not delegated yet"
     * (a needless authorization with a stale EOA nonce). Our own facts win over such reads.
     */
    private val usedEntryPointNonce = ConcurrentHashMap<String, BigInteger>()
    private val delegatedSenders: MutableSet<String> = ConcurrentHashMap.newKeySet()

    fun feeToken(contractAddress: String): FeeTokenConfig? = config.feeToken(contractAddress)

    /** The bundler answers and serves our EntryPoint. */
    fun isAvailable(): Boolean = try {
        rpc.getUserOperationGasPrice()
        true
    } catch (e: Exception) {
        logger.log(Level.WARNING, "AA bundler unavailable: ${e.message}")
        false
    }

    /** `balanceOf(owner)` of [token] as the node sees it now; null when it cannot be read. */
    fun tokenBalance(owner: String, token: String): BigInteger? = try {
        AARpcClient.hexToBig(rpc.ethCall(token, "0x70a08231" + word(owner)))
    } catch (e: Exception) {
        null
    }

    /**
     * Quote for sending [amount] of [token] from [sender] to [recipient] with the fee paid in
     * that same token. Blocking (network).
     */
    @Throws(IOException::class, AARpcException::class)
    fun estimateFee(sender: String, token: FeeTokenConfig, recipient: String, amount: BigInteger): FeeQuote {
        val nonce = getEntryPointNonce(sender)
        // The approval amount is not known yet, but it must not be zero: approve(0) on a fresh
        // allowance writes nothing, while the real approve(max) creates a storage slot — ~120k
        // more call gas on Sepolia. Estimated with 0 the op ran out of gas on-chain, the approve
        // was rolled back and postOp failed with TransferFromFailed (WalletKMP has the same flaw).
        val calls = approvalCalls(token.contractAddress, BigInteger.ONE) + transferCall(token, recipient, amount)
        val skeleton = UserOperation(sender = sender, nonce = nonce, callData = UserOperationEncoder.encodeBatchExecute(calls))

        val gasPrice = rpc.getUserOperationGasPrice()
        val priced = skeleton.copy(maxFeePerGas = gasPrice.maxFeePerGas, maxPriorityFeePerGas = gasPrice.maxPriorityFeePerGas)

        // Estimate without the paymaster: its postOp would try to collect a token fee the
        // simulation cannot account for. The sender gets our delegation code and plenty of ETH
        // for the prefund check, so an undelegated, ETH-less EOA estimates like the real thing.
        // An undelegated EOA also sends an authorization along, and that costs the bundle gas
        // (EIP-8037 state gas on Sepolia: ~57k more preVerificationGas) — without it in the
        // estimate the bundler rejects the op on send.
        val authorization = if (isDelegated(sender)) null else dummyAuthorization(rpc.getTransactionCount(sender))
        val gas = retryWhileBundlerBehind {
            rpc.estimateUserOperationGas(priced, mapOf(sender to StateOverride(
                code = DELEGATION_DESIGNATOR + Numeric.cleanHexPrefix(config.simpleAccountImpl).lowercase(),
                balance = VIRTUAL_ETH_BALANCE)), authorization)
        }
        val op = priced.copy(
            callGasLimit = bumpGas(gas.callGasLimit),
            verificationGasLimit = bumpGas(gas.verificationGasLimit),
            // paymasterAndData is not in the op yet but will be calldata on send
            preVerificationGas = gas.preVerificationGas + PAYMASTER_DATA_GAS_BUFFER
        )

        val result = rpc.paymaster(PM_GET_STUB_DATA, op, token.contractAddress, authorization ?: dummyAuthorization())
        val paymaster = result.requireText("paymaster")
        val paymasterData = Numeric.hexStringToByteArray(result.requireText("paymasterData"))
        val pmVerificationGas = result.hexOrNull("paymasterVerificationGasLimit") ?: DEFAULT_PM_GAS_LIMIT
        val pmPostOpGas = result.hexOrNull("paymasterPostOpGasLimit") ?: DEFAULT_PM_GAS_LIMIT
        val gasCostWei = op.withPaymaster(paymaster, paymasterData, pmVerificationGas, pmPostOpGas).maxCostWei()
        // pm_getPaymasterStubData carries no token amount; derive it from the exchange rate the
        // paymaster put into its own data: tokenAmount = gasCost * rate / 1e18
        val exchangeRate = result.hexOrNull("exchangeRate") ?: exchangeRateFromPaymasterData(paymasterData)
        val tokenAmount = result.hexOrNull("tokenAmount") ?: (gasCostWei * exchangeRate / EXCHANGE_RATE_SCALE)
        val maxTokenAmount = result.hexOrNull("maxTokenAmount")
            ?: (tokenAmount * BigInteger.valueOf(120) / BigInteger.valueOf(100))
        logger.fine("AA quote: call=${op.callGasLimit} verif=${op.verificationGasLimit} preVerif=${op.preVerificationGas} " +
                "pmVerif=$pmVerificationGas pmPostOp=$pmPostOpGas maxFee=${op.maxFeePerGas} rate=$exchangeRate " +
                "auth=${authorization != null} → $tokenAmount (max $maxTokenAmount)")
        return FeeQuote(
            tokenAddress = token.contractAddress,
            tokenSymbol = token.symbol,
            tokenDecimals = token.decimals,
            tokenAmount = tokenAmount,
            maxTokenAmount = maxTokenAmount,
            exchangeRate = exchangeRate,
            gasCostWei = gasCostWei,
            paymasterAddress = paymaster,
            paymasterData = paymasterData,
            paymasterVerificationGasLimit = pmVerificationGas,
            paymasterPostOpGasLimit = pmPostOpGas,
            callGasLimit = op.callGasLimit,
            verificationGasLimit = op.verificationGasLimit,
            preVerificationGas = op.preVerificationGas,
            maxFeePerGas = op.maxFeePerGas,
            maxPriorityFeePerGas = op.maxPriorityFeePerGas,
            validUntil = result.path("validUntil").asLong(0),
            validAfter = result.path("validAfter").asLong(0)
        )
    }

    /**
     * Builds, signs and submits the UserOperation and waits for it to be mined.
     * Blocking (network, up to [RECEIPT_TIMEOUT_MS]).
     */
    fun execute(sender: String, token: FeeTokenConfig, recipient: String, amount: BigInteger,
                quote: FeeQuote, signer: HashSigner): AATransactionResult = try {
        executeInternal(sender, token, recipient, amount, quote, signer)
    } catch (e: AARpcException) {
        logger.log(Level.WARNING, "AA send failed", e)
        AATransactionResult.Failure(classify(e.message), e.message ?: "")
    } catch (e: IOException) {
        AATransactionResult.Failure(AAError.NETWORK_ERROR, e.message ?: "")
    } catch (e: Exception) {
        logger.log(Level.WARNING, "AA send failed", e)
        AATransactionResult.Failure(AAError.UNKNOWN, e.message ?: e.javaClass.simpleName)
    }

    private fun executeInternal(sender: String, token: FeeTokenConfig, recipient: String, amount: BigInteger,
                                quote: FeeQuote, signer: HashSigner): AATransactionResult {
        val now = System.currentTimeMillis() / 1000
        if (quote.validUntil > 0 && now > quote.validUntil || quote.validAfter > 0 && now < quote.validAfter) {
            return AATransactionResult.Failure(AAError.FEE_QUOTE_EXPIRED, "Fee quote outside its validity window")
        }
        // The quote comes from a third party and two of its fields commit the user to something
        // the screen never showed: the approval is built from maxTokenAmount while tokenAmount is
        // displayed, and the paymaster named in it receives that approval. Bound the first and pin
        // the second, so a compromised or MITM-ed service cannot obtain an oversized allowance.
        if (quote.tokenAmount.signum() <= 0) {
            return AATransactionResult.Failure(AAError.FEE_QUOTE_REJECTED, "Fee quote has a non-positive amount")
        }
        if (quote.maxTokenAmount > quote.tokenAmount * MAX_FEE_BUFFER_MULTIPLIER) {
            return AATransactionResult.Failure(AAError.FEE_QUOTE_REJECTED,
                "Approval of ${quote.maxTokenAmount} exceeds ${MAX_FEE_BUFFER_MULTIPLIER}x the quoted fee ${quote.tokenAmount}")
        }
        if (!quote.paymasterAddress.equals(config.paymasterAddress, ignoreCase = true)) {
            return AATransactionResult.Failure(AAError.FEE_QUOTE_REJECTED,
                "Fee quote names paymaster ${quote.paymasterAddress}, expected ${config.paymasterAddress}")
        }

        val calls = approvalCalls(token.contractAddress, quote.maxTokenAmount) + transferCall(token, recipient, amount)
        val nonce = getEntryPointNonce(sender)
        val op = UserOperation(
            sender = sender,
            nonce = nonce,
            callData = UserOperationEncoder.encodeBatchExecute(calls),
            // gas captured during estimation: the bundler rejects an op with zero limits
            callGasLimit = quote.callGasLimit,
            verificationGasLimit = quote.verificationGasLimit,
            preVerificationGas = quote.preVerificationGas,
            maxFeePerGas = quote.maxFeePerGas,
            maxPriorityFeePerGas = quote.maxPriorityFeePerGas
        )

        // Signed before asking the paymaster: it simulates with the delegation in place, without
        // it an undelegated EOA is rejected with AA20. Uses the EOA nonce, not the EntryPoint one.
        val authorization = if (isDelegated(sender)) null else {
            // The authorization is valid only for the EOA nonce current when the bundle executes.
            // With an ETH-paid tx of ours still in the mempool one of the two would be voided —
            // the pending tx dropped, or the authorization skipped and the op failing.
            val latest = rpc.getTransactionCount(sender)
            if (rpc.getTransactionCount(sender, "pending") != latest) {
                return AATransactionResult.Failure(AAError.PENDING_EOA_TX,
                    "An ETH-paid transaction from $sender is still pending")
            }
            signAuthorization(latest, signer)
        }

        // The paymaster signs over paymasterAndData including its gas limits — send it the same
        // limits the op will carry, or validation fails with AA34.
        val stubbed = op.withPaymaster(quote.paymasterAddress, quote.paymasterData,
            quote.paymasterVerificationGasLimit, quote.paymasterPostOpGasLimit)
        val result = rpc.paymaster(PM_GET_DATA, stubbed, token.contractAddress, authorization)
        val paymaster = result.requireText("paymaster")
        // The blob is signed as-is: it has to name the paymaster the allowance goes to.
        if (!paymaster.equals(config.paymasterAddress, ignoreCase = true)) {
            return AATransactionResult.Failure(AAError.FEE_QUOTE_REJECTED,
                "Paymaster data names $paymaster, expected ${config.paymasterAddress}")
        }
        val withPaymaster = op.withPaymaster(paymaster, Numeric.hexStringToByteArray(result.requireText("paymasterData")),
            quote.paymasterVerificationGasLimit, quote.paymasterPostOpGasLimit)

        val hash = UserOperationEncoder.getUserOpHash(withPaymaster, config.entryPointAddress, config.chainId)
        val signed = withPaymaster.copy(signature = signatureBytes(signer(hash)))
        val userOpHash = retryWhileBundlerBehind { rpc.sendUserOperation(signed, authorization) }
        logger.info("UserOperation submitted: $userOpHash")

        // Submitted: the nonce is spent and the authorization (if any) travels with it, whatever
        // the receipt says later.
        val key = sender.lowercase()
        usedEntryPointNonce.merge(key, nonce, BigInteger::max)
        if (authorization != null) delegatedSenders.add(key)

        val receipt = waitForReceipt(userOpHash)
            ?: return AATransactionResult.Failure(AAError.TIMEOUT, "UserOperation $userOpHash not included in time")
        return if (receipt.success) {
            AATransactionResult.Success(receipt.userOpHash, receipt.transactionHash, receipt.blockNumber,
                receipt.tokenDebited(token.contractAddress, sender))
        } else {
            AATransactionResult.Failure(AAError.USER_OP_REVERTED, "UserOperation reverted on-chain (${receipt.transactionHash})")
        }
    }

    /**
     * Runs [call] again while the bundler rejects our nonce as AA25. Right after the previous op
     * of this sender mined, the bundler's simulation node can still be a block behind and expect
     * the old nonce — the second send in a row then fails for nothing (seen 2026-10-29 on
     * Sepolia, and what broke repeated sends in WalletKMP). A block later it agrees with us.
     */
    private fun <T> retryWhileBundlerBehind(call: () -> T): T {
        repeat(STALE_NONCE_RETRIES) {
            try {
                return call()
            } catch (e: AARpcException) {
                if (e.message?.contains("AA25") != true) throw e
                logger.info("bundler behind on the EntryPoint nonce, retrying: ${e.message}")
                Thread.sleep(STALE_NONCE_RETRY_MS)
            }
        }
        return call()
    }

    private fun waitForReceipt(userOpHash: String): UserOpReceipt? {
        val deadline = System.currentTimeMillis() + RECEIPT_TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            try {
                rpc.getUserOperationReceipt(userOpHash)?.let { return it }
            } catch (e: Exception) {
                // transient — keep polling
                logger.log(Level.INFO, "receipt poll: ${e.message}")
            }
            Thread.sleep(RECEIPT_POLL_MS)
        }
        return null
    }

    // ------------------------------------------------------------------ calls

    private fun transferCall(token: FeeTokenConfig, recipient: String, amount: BigInteger) =
        Call(to = token.contractAddress, data = UserOperationEncoder.encodeERC20Transfer(recipient, amount))

    /**
     * `approve(paymaster, 0)` then `approve(paymaster, amount)`: leaves an allowance of exactly
     * [amount] whatever was there before.
     *
     * USDT's approve() reverts when both the current and the new allowance are non-zero, and
     * postOp only takes the actual cost, so every paid op leaves the rest of maxTokenAmount
     * approved — a lone approve() in the next op reverts ("AA50 PostOp Reverted"). The KMP wallet
     * skips the reset when it reads a zero allowance, but on a second send in a row that read can
     * come from a node that has not seen the first op yet. The reset costs a few thousand gas and
     * keeps the shape constant, so estimation always simulates the calls later executed —
     * provided it estimates with a non-zero [amount] too.
     */
    private fun approvalCalls(token: String, amount: BigInteger): List<Call> = listOf(
        Call(to = token, data = UserOperationEncoder.encodeERC20Approve(config.paymasterAddress, BigInteger.ZERO)),
        Call(to = token, data = UserOperationEncoder.encodeERC20Approve(config.paymasterAddress, amount))
    )

    /**
     * `EntryPoint.getNonce(sender, 0)` — the UserOperation nonce, not the EOA one — never below
     * the one after our own last submitted op.
     */
    private fun getEntryPointNonce(sender: String): BigInteger {
        val onChain = AARpcClient.hexToBig(rpc.ethCall(config.entryPointAddress, "0x35567e1a" + word(sender) + "0".repeat(64)))
        val next = usedEntryPointNonce[sender.lowercase()]?.add(BigInteger.ONE) ?: return onChain
        return onChain.max(next)
    }

    private fun word(address: String) = Numeric.cleanHexPrefix(address).lowercase().padStart(64, '0')

    // ------------------------------------------------------------------ EIP-7702

    /**
     * Delegated to *our* implementation. An EOA pointing at someone else's code (an older build,
     * another wallet) would validate the op with that code and revert, so it is re-authorized.
     */
    private fun isDelegated(eoa: String): Boolean = eoa.lowercase() in delegatedSenders || try {
        val code = rpc.getCode(eoa).lowercase()
        code.startsWith(DELEGATION_DESIGNATOR) &&
                ("0x" + code.removePrefix(DELEGATION_DESIGNATOR)).equals(config.simpleAccountImpl, ignoreCase = true)
    } catch (e: Exception) {
        false
    }

    /** keccak256(0x05 || rlp([chainId, delegate, nonce])), signed by the EOA. */
    private fun signAuthorization(eoaNonce: BigInteger, signer: HashSigner): Authorization7702 {
        val delegate = Numeric.hexStringToByteArray(config.simpleAccountImpl)
        val tuple = RlpEncoder.encode(RlpList(
            RlpString.create(BigInteger.valueOf(config.chainId)),
            RlpString.create(delegate),
            RlpString.create(eoaNonce)))
        val signature = signer(Hash.sha3(byteArrayOf(0x05) + tuple))
        val v = signature.v[0].toInt() and 0xff
        return Authorization7702(
            chainId = UserOperationEncoder.hex(BigInteger.valueOf(config.chainId)),
            address = config.simpleAccountImpl,
            nonce = UserOperationEncoder.hex(eoaNonce),
            yParity = UserOperationEncoder.hex(BigInteger.valueOf(((if (v >= 27) v - 27 else v) and 1).toLong())),
            r = UserOperationEncoder.hex(BigInteger(1, signature.r)),
            s = UserOperationEncoder.hex(BigInteger(1, signature.s))
        )
    }

    /**
     * Stand-in for the authorization during estimation: the bundler and paymaster only need the
     * delegate address (and the nonce for gas accounting); r and s just have to be valid scalars.
     */
    private fun dummyAuthorization(eoaNonce: BigInteger = BigInteger.ZERO) = Authorization7702(
        chainId = UserOperationEncoder.hex(BigInteger.valueOf(config.chainId)),
        address = config.simpleAccountImpl,
        nonce = UserOperationEncoder.hex(eoaNonce),
        yParity = "0x0",
        r = "0x" + "aa".repeat(32),
        s = "0x" + "aa".repeat(32)
    )

    /** r || s || v with v = 27/28, as Simple7702Account's ECDSA.recover expects. */
    private fun signatureBytes(signature: Sign.SignatureData): ByteArray {
        val v = signature.v[0].toInt() and 0xff
        val normalizedV = (if (v >= 27) v else v + 27).toByte()
        return Numeric.toBytesPadded(BigInteger(1, signature.r), 32) +
                Numeric.toBytesPadded(BigInteger(1, signature.s), 32) + byteArrayOf(normalizedV)
    }

    // ------------------------------------------------------------------ helpers

    private fun bumpGas(value: BigInteger) = value * BigInteger.valueOf(100L + GAS_LIMIT_BUFFER_PERCENT) / BigInteger.valueOf(100)

    /**
     * ERC-20 mode (0x03) paymasterData: mode(1) validUntil(6) validAfter(6) flags(1) token(20)
     * postOpGas(16) exchangeRate(32) ... — the rate starts at byte 50.
     */
    private fun exchangeRateFromPaymasterData(data: ByteArray): BigInteger {
        if (data.isEmpty() || data[0].toInt() != 0x03 || data.size < 82) {
            throw AARpcException("Unexpected paymasterData, cannot read the exchange rate")
        }
        return BigInteger(1, data.copyOfRange(50, 82)).takeIf { it.signum() > 0 }
            ?: throw AARpcException("Paymaster returned a zero exchange rate")
    }

    private fun JsonNode.requireText(field: String): String =
        path(field).takeIf { it.isTextual }?.asText() ?: throw AARpcException("Paymaster response without '$field'")

    private fun JsonNode.hexOrNull(field: String): BigInteger? =
        path(field).takeIf { it.isTextual }?.let { AARpcClient.hexToBig(it.asText()) }

    private fun classify(message: String?): AAError {
        val m = message?.lowercase() ?: return AAError.UNKNOWN
        return when {
            "insufficient" in m || "balance" in m -> AAError.INSUFFICIENT_TOKEN_BALANCE
            "paymaster" in m -> AAError.PAYMASTER_REJECTED
            "estimat" in m -> AAError.GAS_ESTIMATION_FAILED
            else -> AAError.BUNDLER_REJECTED
        }
    }

    companion object {
        private const val PM_GET_STUB_DATA = "pm_getPaymasterStubData"
        private const val PM_GET_DATA = "pm_getPaymasterData"
        private const val DELEGATION_DESIGNATOR = "0xef0100"
        private const val VIRTUAL_ETH_BALANCE = "0xffffffffffffffffffff"
        /** ~200 bytes of paymasterAndData at 16 gas/byte, with a wide margin. */
        private val PAYMASTER_DATA_GAS_BUFFER = BigInteger.valueOf(30_000)
        /**
         * Pimlico estimates the simulation minimum; cold slots, approvals and postOp can exceed it
         * and revert silently. Applied before the paymaster signs so its signature covers it.
         */
        private const val GAS_LIMIT_BUFFER_PERCENT = 30L
        private val DEFAULT_PM_GAS_LIMIT = BigInteger.valueOf(100_000)
        private val EXCHANGE_RATE_SCALE = BigInteger.TEN.pow(18)
        /** The service quotes ~1.2x to absorb estimation error; 2x leaves room without an open-ended allowance. */
        private val MAX_FEE_BUFFER_MULTIPLIER = BigInteger.valueOf(2)
        const val RECEIPT_TIMEOUT_MS = 120_000L
        /** About two blocks of waiting for the bundler to see our previous op. */
        private const val STALE_NONCE_RETRIES = 6
        private const val STALE_NONCE_RETRY_MS = 4_000L
        private const val RECEIPT_POLL_MS = 3_000L
    }
}
