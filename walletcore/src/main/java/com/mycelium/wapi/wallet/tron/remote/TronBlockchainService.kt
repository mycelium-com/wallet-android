package com.mycelium.wapi.wallet.tron.remote

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.mrd.bitlib.util.HexUtils
import com.mycelium.wapi.wallet.tron.TronAddress
import com.mycelium.wapi.wallet.tron.TronBlockRef
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.math.BigInteger
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger

/**
 * TRON network access over the TronGrid-compatible HTTP API (`api.trongrid.io`,
 * `nile.trongrid.io`, or any java-tron node with the HTTP API enabled).
 *
 * All calls are blocking and throw [IOException] on transport / server errors; callers run
 * them on an IO dispatcher. Endpoints are tried in order; one that answers 429/403 is put on a
 * cooldown so the next call goes to the next endpoint. Each endpoint is rate-limited to what
 * TronGrid tolerates (15 QPS with an API key, ~1 QPS without).
 */
class TronBlockchainService(endpoints: List<TronEndpoint>) : ServerTronListChangedListener {
    private val logger = Logger.getLogger(TronBlockchainService::class.simpleName)
    private val mapper = ObjectMapper()

    @Volatile
    private var endpoints: List<TronEndpoint> = endpoints.sortedBy { it.priority }
        set(value) {
            field = value
            logger.info("TRON endpoints: " + value.joinToString { it.describe() })
        }
    private val gates = HashMap<String, EndpointGate>()

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Block header cache: TAPOS refs stay valid for a long time; refresh at most every 15 s.
    @Volatile
    private var cachedBlockRef: TronBlockRef? = null
    @Volatile
    private var cachedBlockRefAt = 0L

    init {
        logger.info("TRON endpoints: " + this.endpoints.joinToString { it.describe() })
    }

    override fun serverListChanged(newEndpoints: List<TronEndpoint>) {
        if (newEndpoints.isNotEmpty()) endpoints = newEndpoints.sortedBy { it.priority }
    }

    /** Log form without the key itself. */
    private fun TronEndpoint.describe() =
        "$baseUrl(prio=$priority, rps=${allowedRps.toInt()}, key=${if (apiKey.isNullOrBlank()) "no" else "yes"})"

    // ---------------------------------------------------------------- accounts

    @Throws(IOException::class)
    fun getBalance(address: String): TronBalanceResponse {
        val root = get("v1/accounts/$address")
        val data = root.path("data")
        val info = data.firstOrNull() ?: return TronBalanceResponse(BigInteger.ZERO, exists = false)
        val balance = info.path("balance").asLong(0)
        var staked = 0L
        info.path("frozenV2").forEach { staked += it.path("amount").asLong(0) }
        staked += info.path("delegated_frozenV2_balance_for_bandwidth").asLong(0)
        staked += info.path("account_resource").path("delegated_frozenV2_balance_for_energy").asLong(0)
        return TronBalanceResponse(BigInteger.valueOf(balance), exists = true, stakedSun = BigInteger.valueOf(staked))
    }

    /** True when the address has been activated on-chain. Network failure → assume active. */
    fun isAccountActive(address: String): Boolean = try {
        getBalance(address).exists
    } catch (e: IOException) {
        logger.log(Level.WARNING, "isAccountActive($address) failed, assuming active: ${e.message}")
        true
    }

    @Throws(IOException::class)
    fun getAccountResources(address: String): AccountResourceInfo {
        val root = post("wallet/getaccountresource", obj { put("address", address); put("visible", true) })
        return AccountResourceInfo(
            freeNetAvailable = root.path("freeNetLimit").asLong(0) - root.path("freeNetUsed").asLong(0),
            netAvailable = root.path("NetLimit").asLong(0) - root.path("NetUsed").asLong(0),
            energyAvailable = root.path("EnergyLimit").asLong(0) - root.path("EnergyUsed").asLong(0),
            totalNetLimit = root.path("TotalNetLimit").asLong(0),
            totalEnergyLimit = root.path("TotalEnergyLimit").asLong(0),
        )
    }

    // ---------------------------------------------------------------- chain

    /** Latest block reference for TAPOS; cached for [BLOCK_REF_TTL_MS]. */
    @Throws(IOException::class)
    fun getBlockRef(): TronBlockRef {
        val cached = cachedBlockRef
        val now = System.currentTimeMillis()
        if (cached != null && now - cachedBlockRefAt < BLOCK_REF_TTL_MS) return cached
        val root = post("wallet/getnowblock", obj { })
        val blockId = root.path("blockID").asText("")
        val raw = root.path("block_header").path("raw_data")
        if (blockId.length != 64 || raw.isMissingNode) throw IOException("Malformed getnowblock response")
        val ref = TronBlockRef(raw.path("number").asLong(), blockId, raw.path("timestamp").asLong())
        cachedBlockRef = ref
        cachedBlockRefAt = now
        return ref
    }

    @Throws(IOException::class)
    fun getBlockHeight(): Long = getBlockRef().number

    /** Current energy price in sun (chain parameter `getEnergyFee`), [DEFAULT_ENERGY_PRICE] on failure. */
    fun getEnergyPrice(): Long = try {
        val root = get("wallet/getchainparameters")
        root.path("chainParameter").firstOrNull { it.path("key").asText() == "getEnergyFee" }
            ?.path("value")?.asLong(0)?.takeIf { it > 0 } ?: DEFAULT_ENERGY_PRICE
    } catch (e: IOException) {
        logger.log(Level.WARNING, "getEnergyPrice failed: ${e.message}")
        DEFAULT_ENERGY_PRICE
    }

    // ---------------------------------------------------------------- history

    /**
     * Transactions involving [address] (all contract types), newest first.
     * @param minTimestampMs only txs with block_timestamp >= this; 0 = newest page
     * @param fetchAll walk the history via the fingerprint cursor (bounded to [MAX_HISTORY_PAGES])
     */
    @Throws(IOException::class)
    fun getTransactions(address: String, minTimestampMs: Long = 0, fetchAll: Boolean = false): List<TronTx> {
        val result = mutableListOf<TronTx>()
        var fingerprint: String? = null
        var pages = 0
        do {
            val query = StringBuilder("v1/accounts/$address/transactions?limit=$PAGE_SIZE")
            if (minTimestampMs > 0) query.append("&min_timestamp=$minTimestampMs")
            if (fingerprint != null) query.append("&fingerprint=$fingerprint")
            val root = get(query.toString())
            root.path("data").forEach { node -> parseTx(node)?.let { result.add(it) } }
            fingerprint = root.path("meta").path("fingerprint").takeIf { it.isTextual }?.asText()
            pages++
        } while (fetchAll && fingerprint != null && pages < MAX_HISTORY_PAGES)
        return result
    }

    /** TRC20 `Transfer` events of [contractAddress] involving [address], newest first. */
    @Throws(IOException::class)
    fun getTrc20Transfers(
        address: String,
        contractAddress: String,
        minTimestampMs: Long = 0,
        fetchAll: Boolean = false,
    ): List<TronTrc20Transfer> {
        val result = mutableListOf<TronTrc20Transfer>()
        var fingerprint: String? = null
        var pages = 0
        do {
            val query = StringBuilder("v1/accounts/$address/transactions/trc20?limit=$PAGE_SIZE&contract_address=$contractAddress")
            if (minTimestampMs > 0) query.append("&min_timestamp=$minTimestampMs")
            if (fingerprint != null) query.append("&fingerprint=$fingerprint")
            val root = get(query.toString())
            root.path("data").forEach { node ->
                val type = node.path("type").asText("")
                // The endpoint also returns Approval events; storing them would show up in
                // history as bogus transfers of the approval amount.
                if (type != "Transfer") return@forEach
                val tokenInfo = node.path("token_info")
                result.add(
                    TronTrc20Transfer(
                        txid = node.path("transaction_id").asText(),
                        blockTimestampMs = node.path("block_timestamp").asLong(0),
                        from = node.path("from").asText(""),
                        to = node.path("to").asText(""),
                        value = parseBigInt(node.path("value").asText("0")),
                        contractAddress = tokenInfo.path("address").asText(contractAddress),
                        symbol = tokenInfo.path("symbol").asText(""),
                        decimals = tokenInfo.path("decimals").asInt(0),
                        type = type,
                    )
                )
            }
            fingerprint = root.path("meta").path("fingerprint").takeIf { it.isTextual }?.asText()
            pages++
        } while (fetchAll && fingerprint != null && pages < MAX_HISTORY_PAGES)
        return result
    }

    @Throws(IOException::class)
    fun getTransactionInfo(txId: String): TronTransactionInfo {
        val root = post("wallet/gettransactioninfobyid", obj { put("value", txId) })
        val receipt = root.path("receipt")
        return TronTransactionInfo(
            id = root.path("id").takeIf { it.isTextual }?.asText(),
            blockNumber = root.path("blockNumber").asLong(0),
            blockTimestampMs = root.path("blockTimeStamp").asLong(0),
            fee = root.path("fee").asLong(0),
            receiptResult = receipt.path("result").takeIf { it.isTextual }?.asText(),
            energyUsageTotal = receipt.path("energy_usage_total").asLong(0),
            netUsage = receipt.path("net_usage").asLong(0),
            netFee = receipt.path("net_fee").asLong(0),
            energyFee = receipt.path("energy_fee").asLong(0),
        )
    }

    // ---------------------------------------------------------------- broadcast

    /** Broadcasts a fully signed `Transaction` protobuf via `wallet/broadcasthex`. */
    @Throws(IOException::class)
    fun broadcast(signedTx: ByteArray): SendResult {
        val root = post("wallet/broadcasthex", obj { put("transaction", HexUtils.toHex(signedTx)) })
        val ok = root.path("result").asBoolean(false)
        val code = root.path("code").takeIf { it.isTextual }?.asText()
        val message = root.path("message").takeIf { it.isTextual }?.asText()?.let { decodeMessage(it) }
        val txid = root.path("txid").takeIf { it.isTextual }?.asText()
        // DUP_TRANSACTION = the node already knows this tx (e.g. a retried broadcast).
        // Treating it as a failure would make the user re-send and double-spend.
        if (!ok && code == "DUP_TRANSACTION") return SendResult(true, txid, message, code)
        return SendResult(ok, txid, message ?: code, code)
    }

    // ---------------------------------------------------------------- contracts

    @Throws(IOException::class)
    fun triggerConstantContract(
        ownerAddress: String,
        contractAddress: String,
        functionSelector: String,
        parameterHex: String,
    ): TriggerConstantResult {
        val root = post("wallet/triggerconstantcontract", obj {
            put("owner_address", ownerAddress)
            put("contract_address", contractAddress)
            put("function_selector", functionSelector)
            put("parameter", parameterHex)
            put("visible", true)
        })
        val res = root.path("result")
        // A reverted simulation still reports result=true; the failure shows up as
        // result.message ("REVERT opcode executed") and transaction.ret[0].ret = "FAILED".
        val ret = root.path("transaction").path("ret").firstOrNull()
        val contractRet = ret?.path("contractRet")?.takeIf { it.isTextual }?.asText()
            ?: ret?.path("ret")?.takeIf { it.isTextual }?.asText()
        return TriggerConstantResult(
            success = res.path("result").asBoolean(false),
            message = res.path("message").takeIf { it.isTextual }?.asText()?.let { decodeMessage(it) },
            energyUsed = root.path("energy_used").asLong(0),
            constantResult = root.path("constant_result").map { it.asText() },
            contractRet = contractRet,
        )
    }

    /** `balanceOf(address)` on [contractAddress]; null when the call failed. */
    fun getTrc20Balance(ownerAddress: String, contractAddress: String): BigInteger? = try {
        val owner = TronAddress.decode(ownerAddress) ?: throw IOException("Invalid owner address")
        val param = HexUtils.toHex(owner.copyOfRange(1, 21)).padStart(64, '0')
        val result = triggerConstantContract(ownerAddress, contractAddress, "balanceOf(address)", param)
        if (result.success && result.constantResult.isNotEmpty()) decodeUint(result.constantResult.first()) else null
    } catch (e: Exception) {
        logger.log(Level.WARNING, "getTrc20Balance failed: ${e.message}")
        null
    }

    /**
     * Simulates a TRC20 `transfer` to estimate energy. Falls back to [DEFAULT_TRC20_ENERGY]
     * when the simulation reverts (e.g. sender has no balance yet) — a reverted run reports
     * an energy figure that is not representative of the real transfer.
     */
    fun estimateTrc20TransferEnergy(ownerAddress: String, contractAddress: String, toAddress: String, amount: BigInteger): Long = try {
        val to = TronAddress.decode(toAddress) ?: throw IOException("Invalid recipient address")
        val param = HexUtils.toHex(to.copyOfRange(1, 21)).padStart(64, '0') + amount.toString(16).padStart(64, '0')
        val result = triggerConstantContract(ownerAddress, contractAddress, "transfer(address,uint256)", param)
        val reverted = result.message != null || (result.contractRet != null && result.contractRet != "SUCCESS")
        if (result.success && result.energyUsed > 0 && !reverted) {
            result.energyUsed
        } else {
            logger.log(Level.WARNING, "Energy estimation failed (ret=${result.contractRet}): ${result.message}")
            DEFAULT_TRC20_ENERGY
        }
    } catch (e: Exception) {
        logger.log(Level.WARNING, "estimateTrc20TransferEnergy failed: ${e.message}")
        DEFAULT_TRC20_ENERGY
    }

    /** name()/symbol()/decimals() of a TRC20 contract; null if any call fails. */
    fun getTrc20TokenInfo(contractAddress: String): TRC20TokenInfo? = try {
        val name = constantString(contractAddress, "name()")
        val symbol = constantString(contractAddress, "symbol()")
        val decimals = triggerConstantContract(contractAddress, contractAddress, "decimals()", "")
            .takeIf { it.success && it.constantResult.isNotEmpty() }?.let { decodeUint(it.constantResult.first()) }
        if (name != null && symbol != null && decimals != null) {
            TRC20TokenInfo(contractAddress, name, symbol, decimals.toInt())
        } else null
    } catch (e: Exception) {
        logger.log(Level.WARNING, "getTrc20TokenInfo failed: ${e.message}")
        null
    }

    private fun constantString(contractAddress: String, selector: String): String? {
        val result = triggerConstantContract(contractAddress, contractAddress, selector, "")
        if (!result.success || result.constantResult.isEmpty()) return null
        return decodeAbiString(result.constantResult.first())
    }

    // ---------------------------------------------------------------- parsing

    private fun parseTx(node: JsonNode): TronTx? {
        val txid = node.path("txID").takeIf { it.isTextual }?.asText() ?: return null
        val contract = node.path("raw_data").path("contract").firstOrNull() ?: return null
        val type = contract.path("type").takeIf { it.isTextual }?.asText()
        val v = contract.path("parameter").path("value")
        val owner = TronAddress.fromHex(v.path("owner_address").asText(""))
        val to: String
        val value: Long
        when (type) {
            "TransferContract" -> {
                to = TronAddress.fromHex(v.path("to_address").asText(""))
                value = v.path("amount").asLong(0)
            }
            "FreezeBalanceV2Contract", "FreezeBalanceContract" -> {
                to = ""; value = v.path("frozen_balance").asLong(0)
            }
            "UnfreezeBalanceV2Contract", "UnfreezeBalanceContract" -> {
                to = ""; value = v.path("unfreeze_balance").asLong(0)
            }
            "DelegateResourceContract", "UnDelegateResourceContract" -> {
                to = TronAddress.fromHex(v.path("receiver_address").asText(""))
                value = v.path("balance").asLong(0)
            }
            "VoteWitnessContract" -> {
                to = ""; value = 0
            }
            "TriggerSmartContract", "CreateSmartContract" -> {
                to = TronAddress.fromHex(v.path("contract_address").asText(""))
                value = v.path("call_value").asLong(0)
            }
            "WithdrawBalanceContract" -> {
                to = ""; value = node.path("withdraw_amount").asLong(0)
            }
            "WithdrawExpireUnfreezeContract" -> {
                to = ""; value = node.path("withdraw_expire_amount").asLong(0)
            }
            else -> {
                to = TronAddress.fromHex(v.path("to_address").asText(""))
                value = v.path("amount").asLong(0)
            }
        }
        val contractRet = node.path("ret").firstOrNull()?.path("contractRet")?.asText("SUCCESS") ?: "SUCCESS"
        val fee = node.path("net_fee").asLong(0) + node.path("energy_fee").asLong(0)
        return TronTx(
            txid = txid,
            blockNumber = node.path("blockNumber").asLong(0),
            blockTimestampMs = node.path("block_timestamp").asLong(0),
            from = owner,
            to = to,
            value = BigInteger.valueOf(value),
            fee = BigInteger.valueOf(fee),
            success = contractRet == "SUCCESS",
            contractType = type,
            energyUsage = node.path("energy_usage_total").asLong(0),
            netUsage = node.path("net_usage").asLong(0),
        )
    }

    // ---------------------------------------------------------------- transport

    @Throws(IOException::class)
    private fun get(path: String): JsonNode = execute { endpoint ->
        Request.Builder().url(endpoint.baseUrl.trimEnd('/') + "/" + path).get()
    }

    @Throws(IOException::class)
    private fun post(path: String, body: ObjectNode): JsonNode = execute { endpoint ->
        Request.Builder()
            .url(endpoint.baseUrl.trimEnd('/') + "/" + path)
            .post(body.toString().toRequestBody(JSON))
    }

    @Throws(IOException::class)
    private fun execute(build: (TronEndpoint) -> Request.Builder): JsonNode {
        val list = endpoints
        if (list.isEmpty()) throw IOException("No TRON endpoints configured")
        var lastError: IOException? = null
        val now = System.currentTimeMillis()
        for (endpoint in list) {
            val gate = gateFor(endpoint)
            if (gate.isCoolingDown(now)) continue
            try {
                gate.awaitSlot()
                val builder = build(endpoint).header("Accept", "application/json")
                endpoint.apiKey?.takeIf { it.isNotBlank() }?.let { builder.header(endpoint.apiKeyHeader, it) }
                client.newCall(builder.build()).execute().use { response ->
                    if (response.code == 429 || response.code == 403) {
                        gate.coolDown(System.currentTimeMillis())
                        throw IOException("Rate limited by ${endpoint.baseUrl}: ${response.code}")
                    }
                    val bodyText = response.body?.string() ?: throw IOException("Empty body from ${endpoint.baseUrl}")
                    if (!response.isSuccessful) {
                        throw IOException("HTTP ${response.code} from ${endpoint.baseUrl}: ${bodyText.take(200)}")
                    }
                    return mapper.readTree(bodyText) ?: throw IOException("Empty JSON")
                }
            } catch (e: IOException) {
                logger.log(Level.WARNING, "TRON request failed on ${endpoint.baseUrl}: ${e.message}")
                lastError = e
            } catch (e: RuntimeException) {
                logger.log(Level.WARNING, "TRON request failed on ${endpoint.baseUrl}: ${e.message}")
                lastError = IOException(e)
            }
        }
        throw lastError ?: IOException("All TRON endpoints are cooling down")
    }

    @Synchronized
    private fun gateFor(endpoint: TronEndpoint): EndpointGate =
        gates.getOrPut(endpoint.apiKey?.takeIf { it.isNotBlank() } ?: endpoint.baseUrl) {
            EndpointGate(endpoint.allowedRps)
        }

    /** Per-endpoint minimum spacing between requests plus a penalty cooldown after 429/403. */
    private class EndpointGate(rps: Double) {
        private val minIntervalMs = (1000.0 / rps).toLong()
        private var lastRequestAt = 0L
        @Volatile
        private var coolingDownUntil = 0L

        fun isCoolingDown(now: Long) = now < coolingDownUntil

        fun coolDown(now: Long) {
            coolingDownUntil = now + COOLDOWN_MS
        }

        fun awaitSlot() {
            val wait: Long
            synchronized(this) {
                val now = System.currentTimeMillis()
                val next = maxOf(now, lastRequestAt + minIntervalMs)
                wait = next - now
                lastRequestAt = next
            }
            if (wait > 0) Thread.sleep(wait)
        }
    }

    private inline fun obj(fill: ObjectNode.() -> Unit): ObjectNode = mapper.createObjectNode().apply(fill)

    private fun JsonNode.firstOrNull(): JsonNode? = if (this is ArrayNode && size() > 0) get(0) else null

    private fun JsonNode.firstOrNull(predicate: (JsonNode) -> Boolean): JsonNode? =
        if (this is ArrayNode) this.asSequence().firstOrNull(predicate) else null

    private fun parseBigInt(text: String): BigInteger = try {
        BigInteger(text)
    } catch (e: NumberFormatException) {
        BigInteger.ZERO
    }

    private fun decodeUint(hex: String): BigInteger? = try {
        val clean = hex.removePrefix("0x").trimStart('0').ifEmpty { "0" }
        BigInteger(clean, 16)
    } catch (e: NumberFormatException) {
        null
    }

    /** ABI-decodes a dynamic `string` return value (offset, length, data). */
    private fun decodeAbiString(hex: String): String? = try {
        val clean = hex.removePrefix("0x")
        if (clean.length < 128) {
            // Some contracts return bytes32 for name()/symbol()
            String(HexUtils.toBytes(clean.take(64)), Charsets.UTF_8).trim { it <= ' ' || it == ' ' }.ifEmpty { null }
        } else {
            val length = clean.substring(64, 128).toLong(16).toInt()
            if (length <= 0 || length > 256 || clean.length < 128 + length * 2) null
            else String(HexUtils.toBytes(clean.substring(128, 128 + length * 2)), Charsets.UTF_8).trim()
        }
    } catch (e: Exception) {
        null
    }

    /** java-tron reports messages hex-encoded (ASCII); leave as-is when not hex. */
    private fun decodeMessage(message: String): String = try {
        if (message.matches(Regex("^[0-9a-fA-F]+$")) && message.length % 2 == 0)
            String(HexUtils.toBytes(message), Charsets.UTF_8) else message
    } catch (e: Exception) {
        message
    }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()
        private const val PAGE_SIZE = 100
        private const val MAX_HISTORY_PAGES = 5
        private const val BLOCK_REF_TTL_MS = 15_000L
        private const val COOLDOWN_MS = 35_000L
        const val DEFAULT_ENERGY_PRICE = 420L
        /** Conservative TRC20 transfer energy (USDT to a fresh recipient ≈ 130k). */
        const val DEFAULT_TRC20_ENERGY = 130_000L
    }
}
