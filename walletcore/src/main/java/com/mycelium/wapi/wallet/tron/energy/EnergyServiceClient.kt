package com.mycelium.wapi.wallet.tron.energy

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.math.BigDecimal
import java.math.BigInteger
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger

/**
 * HTTP client of the TRON energy service partner ("pay TRC20 fees in USDT"). The partner
 * sponsors the energy of a TRC20 transfer and gets paid with a token transfer to its fee
 * wallet; both transactions are signed by the wallet and broadcast by the partner.
 *
 * Endpoints (all JSON, `X-API-Key` header):
 *  - `GET  health`
 *  - `GET  api/v1/fee-info?sender&recipient&token_contract&amount` (amount in human units)
 *  - `POST api/v1/transfer/execute` `{transaction_id, signed_tx_fee, signed_tx_main}`
 *  - `GET  api/v1/order/{order_id}/status`
 *
 * Blocking calls; throw [EnergyServiceException] on 4xx/5xx and [IOException] on transport errors.
 */
class EnergyServiceClient(
    private val config: EnergyServiceConfig,
    private val userAgent: String? = null,
    private val acceptLanguage: String? = null,
) {
    private val logger = Logger.getLogger(EnergyServiceClient::class.simpleName)
    private val mapper = ObjectMapper()
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val isConfigured: Boolean get() = config.isConfigured

    @Throws(IOException::class, EnergyServiceException::class)
    fun health(): EnergyServiceHealth {
        val root = get("health")
        return EnergyServiceHealth(
            status = root.path("status").asText(""),
            networkMode = root.path("network").path("mode").asText(""),
            balanceEnergy = root.path("balance_energy").asBoolean(false),
            balanceBandwidth = root.path("balance_bandwidth").asBoolean(false),
            supportedTokens = root.path("supported_tokens").map { it.path("contract").asText("") }.filter { it.isNotEmpty() },
        )
    }

    /**
     * @param amount token amount in human units (e.g. 1.5 USDT)
     */
    @Throws(IOException::class, EnergyServiceException::class)
    fun feeInfo(sender: String, recipient: String, tokenContract: String, amount: BigDecimal): EnergyFeeQuote {
        val root = get("api/v1/fee-info?sender=$sender&recipient=$recipient&token_contract=$tokenContract" +
                "&amount=${amount.stripTrailingZeros().toPlainString()}")
        val checks = root.path("checks")
        val expiresIn = root.path("expires_in").asInt(600)
        return EnergyFeeQuote(
            transactionId = root.path("transaction_id").asInt(),
            feeAmount = root.path("fee_amount").asText("0"),
            feeAmountUnits = BigInteger.valueOf(root.path("fee_amount_sun").asLong(0)),
            tokenSymbol = root.path("token_symbol").asText(""),
            tokenContract = root.path("token_contract").asText(tokenContract),
            tokenDecimals = root.path("token_decimals").asInt(6),
            transferMethod = root.path("transfer_method").asText("two_transactions"),
            feeWallet = root.path("fee_wallet").takeIf { it.isTextual }?.asText(),
            energyNeeded = root.path("energy_needed").asLong(0),
            bandwidthNeeded = root.path("bandwidth_needed").asLong(0),
            recipientHasToken = root.path("recipient_has_token").asBoolean(false),
            minFeeLimit = root.path("min_fee_limit").asLong(0),
            expiresAtMs = System.currentTimeMillis() + expiresIn * 1000L,
            estimatedTimeSec = root.path("estimated_time").takeIf { it.isNumber }?.asInt(),
            balanceOk = if (checks.path("balance").isMissingNode) root.path("success").asBoolean(true) else checks.path("balance").path("ok").asBoolean(false),
            balanceMessage = checks.path("balance").path("message").takeIf { it.isTextual }?.asText(),
        )
    }

    /**
     * Hands both signed transactions to the partner. Hex payloads are the TrustWallet-Core-style
     * `SigningOutput` protobufs ([com.mycelium.wapi.wallet.tron.TronTransaction.signingOutput]).
     */
    @Throws(IOException::class, EnergyServiceException::class)
    fun execute(transactionId: Int, signedFeeTxHex: String, signedMainTxHex: String): EnergyExecuteResponse {
        val body = mapper.createObjectNode().apply {
            put("transaction_id", transactionId)
            putNull("signed_tx")
            put("signed_tx_fee", signedFeeTxHex)
            put("signed_tx_main", signedMainTxHex)
        }
        val root = post("api/v1/transfer/execute", body.toString())
        return EnergyExecuteResponse(
            success = root.path("success").asBoolean(true),
            orderId = root.path("order_id").asText(""),
            status = root.path("status").asText("pending"),
            message = root.path("message").asText(""),
        )
    }

    @Throws(IOException::class, EnergyServiceException::class)
    fun orderStatus(orderId: String): EnergyOrderStatus {
        val root = get("api/v1/order/$orderId/status")
        return EnergyOrderStatus(
            orderId = root.path("order_id").asText(orderId),
            status = root.path("status").asText(""),
            txHash = root.path("tx_hash").takeIf { it.isTextual }?.asText(),
            feeTxHash = root.path("tx2_hash").takeIf { it.isTextual }?.asText(),
            errorCode = root.path("error_code").takeIf { it.isTextual }?.asText(),
            errorMessage = root.path("error_message").takeIf { it.isTextual }?.asText(),
        )
    }

    /**
     * Polls the order until it is terminal or [timeoutMs] elapses. A timeout yields
     * [EnergyTransferResult.Pending] rather than an error: reporting a failure here would make
     * the user re-send and pay twice. Three consecutive 404s mean the order is gone.
     */
    fun pollOrderUntilTerminal(
        orderId: String,
        onStatus: (EnergyOrderStatus) -> Unit = {},
        intervalMs: Long = POLL_INTERVAL_MS,
        timeoutMs: Long = POLL_TIMEOUT_MS,
    ): EnergyTransferResult {
        val start = System.currentTimeMillis()
        var notFound = 0
        while (true) {
            if (System.currentTimeMillis() - start >= timeoutMs) return EnergyTransferResult.Pending(orderId)
            try {
                val status = orderStatus(orderId)
                notFound = 0
                onStatus(status)
                if (status.isTerminal) {
                    return if (status.isSuccess) {
                        EnergyTransferResult.Success(status.txHash ?: orderId, status.feeTxHash)
                    } else {
                        EnergyTransferResult.Error(status.errorMessage ?: "Transaction failed (${status.status})")
                    }
                }
            } catch (e: EnergyServiceException) {
                if (e.isNotFound) {
                    notFound++
                    if (notFound >= NOT_FOUND_TERMINAL_THRESHOLD) return EnergyTransferResult.Error("Order not found", e)
                } else {
                    logger.log(Level.WARNING, "order status failed: ${e.message}")
                }
            } catch (e: IOException) {
                logger.log(Level.WARNING, "order status failed: ${e.message}")
            }
            try {
                Thread.sleep(intervalMs)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                return EnergyTransferResult.Pending(orderId)
            }
        }
    }

    // ------------------------------------------------------------------ transport

    private fun get(path: String): JsonNode = execute(request(path).get().build())

    private fun post(path: String, json: String): JsonNode =
        execute(request(path).post(json.toRequestBody(JSON)).build())

    private fun request(path: String): Request.Builder {
        val builder = Request.Builder()
            .url(config.baseUrl.trimEnd('/') + "/" + path)
            .header("Accept", "application/json")
            .header("X-API-Key", config.apiKey)
        userAgent?.let { builder.header("User-Agent", it) }
        acceptLanguage?.let { builder.header("Accept-Language", it) }
        return builder
    }

    @Throws(IOException::class, EnergyServiceException::class)
    private fun execute(request: Request): JsonNode {
        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) throw parseError(response.code, body)
            return try {
                mapper.readTree(body) ?: mapper.createObjectNode()
            } catch (e: Exception) {
                throw IOException("Malformed response: ${e.message}")
            }
        }
    }

    private fun parseError(code: Int, body: String): EnergyServiceException = try {
        val detail = mapper.readTree(body)?.path("detail")
        when {
            detail == null || detail.isMissingNode -> EnergyServiceException(code, body.ifBlank { "HTTP $code" })
            detail.isObject -> EnergyServiceException(code, detail.path("message").takeIf { it.isTextual }?.asText() ?: body,
                detail.path("error").takeIf { it.isTextual }?.asText())
            else -> EnergyServiceException(code, detail.asText())
        }
    } catch (e: Exception) {
        EnergyServiceException(code, "HTTP $code")
    }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()
        const val POLL_INTERVAL_MS = 10_000L
        const val POLL_TIMEOUT_MS = 120_000L
        private const val NOT_FOUND_TERMINAL_THRESHOLD = 3
    }
}
