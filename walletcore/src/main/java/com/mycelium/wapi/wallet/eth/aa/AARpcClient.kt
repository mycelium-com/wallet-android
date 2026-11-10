package com.mycelium.wapi.wallet.eth.aa

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.web3j.utils.Numeric
import java.io.IOException
import java.math.BigInteger
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

/**
 * JSON-RPC of the three endpoints the AA flow talks to: the bundler (Pimlico Alto), the
 * ERC-7677 paymaster (`pm_*`, by default served by the bundler URL) and a plain Ethereum node.
 *
 * Blocking; throws [AARpcException] on a JSON-RPC error and [IOException] on transport errors.
 */
class AARpcClient(private val config: ChainAAConfig) {
    private val logger = Logger.getLogger(AARpcClient::class.simpleName)
    private val mapper = ObjectMapper()
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        // no silent re-POST after a lost response: the surfaced error must belong to the request
        // that may have reached the bundler (see isProvablyNotSent)
        .retryOnConnectionFailure(false)
        .build()

    // ------------------------------------------------------------------ bundler

    fun getUserOperationGasPrice(): GasPrice {
        val result = call(config.bundlerUrl, "pimlico_getUserOperationGasPrice", emptyList())
        // { slow, standard, fast } — like the KMP wallet, take "fast"
        val fast = result.path("fast").takeUnless { it.isMissingNode } ?: result
        return GasPrice(fast.hexBig("maxFeePerGas"), fast.hexBig("maxPriorityFeePerGas"))
    }

    /**
     * [authorization]: an EIP-7702 authorization makes the bundle more expensive (it is part of the
     * type-4 transaction and creates state), which only shows up in preVerificationGas if the
     * bundler sees it during estimation.
     */
    fun estimateUserOperationGas(userOp: UserOperation, stateOverrides: Map<String, StateOverride>,
                                 authorization: Authorization7702? = null): GasEstimate {
        val overrides = stateOverrides.mapValues { (_, o) ->
            linkedMapOf<String, Any>().apply {
                o.code?.let { put("code", it) }
                o.balance?.let { put("balance", it) }
            }
        }
        val op = UserOperationEncoder.toRpcMap(userOp)
        authorization?.let { op["eip7702Auth"] = it.toRpc() }
        val result = call(config.bundlerUrl, "eth_estimateUserOperationGas",
            listOf(op, config.entryPointAddress, overrides))
        return GasEstimate(result.hexBig("callGasLimit"), result.hexBig("verificationGasLimit"),
            result.hexBig("preVerificationGas"))
    }

    /** Pimlico expects `[userOp, entryPoint]` with the EIP-7702 authorization inside the userOp. */
    fun sendUserOperation(userOp: UserOperation, authorization: Authorization7702?): String {
        val op = UserOperationEncoder.toRpcMap(userOp)
        authorization?.let { op["eip7702Auth"] = it.toRpc() }
        return call(config.bundlerUrl, "eth_sendUserOperation", listOf(op, config.entryPointAddress))
            .asText().ifEmpty { throw AARpcException("eth_sendUserOperation returned no hash") }
    }

    /** Null while the UserOperation is not included yet. */
    fun getUserOperationReceipt(userOpHash: String): UserOpReceipt? {
        val result = call(config.bundlerUrl, "eth_getUserOperationReceipt", listOf(userOpHash))
        if (result.isNull || result.isMissingNode) return null
        val txHash = result.path("receipt").path("transactionHash").asText(null)
            ?: result.path("transactionHash").asText(null)
            ?: throw AARpcException("Receipt without transactionHash")
        return UserOpReceipt(
            userOpHash = result.path("userOpHash").asText(userOpHash),
            transactionHash = txHash,
            success = result.path("success").asBoolean(false),
            actualGasCost = result.hexBig("actualGasCost"),
            blockNumber = hexToBig(result.path("receipt").path("blockNumber").asText("0x0")).toLong(),
            // the op's own logs (not the whole bundle's)
            logs = result.path("logs").map { log ->
                ReceiptLog(log.path("address").asText(""), log.path("topics").map { it.asText("") }, log.path("data").asText("0x"))
            }
        )
    }

    // ------------------------------------------------------------------ paymaster (ERC-7677)

    /** `[userOp, entryPoint, chainId, {token}]` — the authorization lets it simulate undelegated EOAs. */
    fun paymaster(method: String, userOp: UserOperation, token: String, authorization: Authorization7702?): JsonNode {
        val op = UserOperationEncoder.toRpcMap(userOp)
        authorization?.let { op["eip7702Auth"] = it.toRpc() }
        return call(config.effectivePaymasterRpcUrl, method,
            listOf(op, config.entryPointAddress, chainIdHex(), mapOf("token" to token)))
    }

    // ------------------------------------------------------------------ plain node

    fun ethCall(to: String, data: String): String =
        call(config.ethRpcUrl, "eth_call", listOf(mapOf("to" to to, "data" to data), "latest")).asText("0x")

    fun getCode(address: String): String =
        call(config.ethRpcUrl, "eth_getCode", listOf(address, "latest")).asText("0x")

    fun getTransactionCount(address: String, block: String = "latest"): BigInteger =
        hexToBig(call(config.ethRpcUrl, "eth_getTransactionCount", listOf(address, block)).asText("0x0"))

    // ------------------------------------------------------------------

    private fun chainIdHex() = "0x" + config.chainId.toString(16)

    private fun Authorization7702.toRpc() = linkedMapOf(
        "contractAddress" to address,
        "chainId" to chainId,
        "nonce" to nonce,
        "r" to r,
        "s" to s,
        "yParity" to yParity
    )

    @Throws(IOException::class, AARpcException::class)
    private fun call(url: String, method: String, params: List<Any>): JsonNode {
        val body = mapper.writeValueAsString(mapOf("jsonrpc" to "2.0", "id" to 1, "method" to method, "params" to params))
        val request = Request.Builder().url(url)
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            val root = try {
                mapper.readTree(text)
            } catch (e: Exception) {
                throw IOException("$method: HTTP ${response.code}")
            }
            val error = root.path("error")
            if (!error.isMissingNode && !error.isNull) {
                val message = error.path("message").asText("Unknown error")
                logger.warning("$method failed: $message")
                throw AARpcException(message, error.path("code").takeIf { it.isInt }?.asInt())
            }
            if (!response.isSuccessful) throw IOException("$method: HTTP ${response.code}")
            return root.path("result")
        }
    }

    private fun JsonNode.hexBig(field: String): BigInteger = hexToBig(path(field).asText("0x0"))

    companion object {
        fun hexToBig(hex: String?): BigInteger {
            val clean = Numeric.cleanHexPrefix(hex ?: "")
            return if (clean.isEmpty()) BigInteger.ZERO else BigInteger(clean, 16)
        }
    }
}
