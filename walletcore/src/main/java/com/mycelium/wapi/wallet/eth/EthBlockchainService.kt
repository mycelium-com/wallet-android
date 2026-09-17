package com.mycelium.wapi.wallet.eth

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.mycelium.net.HttpEndpoint
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.IOException
import java.math.BigDecimal
import java.math.BigInteger
import java.util.concurrent.TimeUnit

/**
 * @param eip1559EstimatorUrl dedicated EIP-1559 gas estimator, injected by the app
 *        (BuildConfig.Eip1559Estimator) rather than compiled in — the URL that used
 *        to be hardcoded here pointed at a decommissioned host, and since every
 *        failure on this path is swallowed, the only symptom was that type-2
 *        signing silently never happened. Blank is fine: [EthFeeProvider] then
 *        derives the snapshot from the base fee instead.
 */
class EthBlockchainService @JvmOverloads constructor(
    private var endpoints: List<HttpEndpoint>,
    private val eip1559EstimatorUrl: String = "",
) : ServerEthListChangedListener {
    private val mapper = ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    // Last successful EIP-1559 fee snapshot. Populated by EthFeeProvider's
    // refresh and read non-blocking at tx-build time to recover the tip that
    // matches a chosen maxFeePerGas cap (the fee picker only carries the cap).
    @Volatile
    private var lastEip1559: Eip1559Snapshot? = null

    // One shared connection pool/dispatcher for every call. A new OkHttpClient per request
    // kept its own pool alive for 5 min, so each sync (up to ~20 getTransaction calls per
    // account, triggered on every resume) leaked hundreds of TLS sessions until OOM.
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    private val streamingClient = httpClient.newBuilder()
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    private val sendClient = streamingClient.newBuilder()
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun client(urlString: String): String {
        val request = Request.Builder().url(urlString).build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            return response.body?.string() ?: throw IOException("Empty body")
        }
    }

    // Stream-parse to avoid OOM on accounts with large tx histories (loading
    // the full body as a String can require 100+ MB before parsing starts).
    private fun <T> streamedRequest(urlString: String, type: Class<T>): T {
        val request = Request.Builder().url(urlString).build()
        val response = streamingClient.newCall(request).execute()
        if (!response.isSuccessful) {
            response.close()
            throw IOException("Unexpected code $response")
        }
        val body = response.body ?: throw IOException("Empty body")
        return body.use { mapper.readValue(it.byteStream(), type) }
    }

    @Throws(IOException::class)
    private fun fetchTransactions(
        address: String,
        contractAddress: String? = null,
        knownConfirmedTxids: Set<String> = emptySet(),
    ): List<Tx> {
        // Blockbook has a bug with `&contract=...` filtering — server returns 0 txs/balance.
        // Fetch all address txs and rely on client-side filtering in getTransactions().
        // val contractAddressSegment = if (contractAddress != null) "&contract=$contractAddress" else ""
        // Small page size keeps each response bounded — on busy addresses a
        // page=200 response can be megabytes of JSON, and parsing it on the
        // single-threaded JSON path dominates the incremental-sync wall clock.
        // 50 matches getAddressTxids and is comfortably larger than the rate
        // at which new txs land between syncs, so incremental sweeps still
        // stop after the first page in the common case.
        val pageSize = 50
        var urlString = "${endpoints.random()}/api/v2/address/$address?details=txs&pageSize=$pageSize" //+ contractAddressSegment

        val result: MutableList<Tx> = mutableListOf()

        val initialResponse = streamedRequest(urlString, Response::class.java)
        result.addAll(initialResponse.transactions)
        // Incremental sync: blockbook orders newest-first. As soon as a page
        // contains a confirmed tx the caller already has locally, we know
        // everything older is already stored too — stop paginating. Pending
        // txs (confirmations == 0) don't trigger the stop: they can move
        // between pages and must always be (re-)observed at the top.
        // Callers pass an empty set to force a full sweep (initial sync).
        if (knownConfirmedTxids.isNotEmpty()
            && initialResponse.transactions.any { it.confirmations.signum() > 0 && it.txid in knownConfirmedTxids }) {
            return result
        }
        for (i in 2..initialResponse.totalPages) {
            urlString = "${endpoints.random()}/api/v2/address/$address?details=txs&pageSize=$pageSize&page=$i" //+ contractAddressSegment
            val response = streamedRequest(urlString, Response::class.java)
            result.addAll(response.transactions)
            if (knownConfirmedTxids.isNotEmpty()
                && response.transactions.any { it.confirmations.signum() > 0 && it.txid in knownConfirmedTxids }) {
                break
            }
        }
        return result
    }

    fun sendTransaction(hex: String): SendResult {
        val request = Request.Builder()
                .url("${endpoints.random()}/api/v2/sendtx/")
                .post(RequestBody.create(null, hex))
                .build()
        val result = sendClient.newCall(request).execute().use { response ->
            mapper.readValue(response.body!!.string(), SendTxResponse::class.java)
        }
        return SendResult(result.result != null, result.error)
    }

    fun getBlockHeight(): BigInteger {
        // /api/v2 (not /api/) — returns the same {"blockbook":{"bestHeight":...}}
        // payload, but the bare /api/ path is blocked by Cloudflare on the
        // public Trezor blockbook instances.
        val urlString = "${endpoints.random()}/api/v2"

        return mapper.readValue(client(urlString), ApiResponse::class.java).blockbook!!.bestHeight
    }

    fun getNonce(address: String): BigInteger {
        val urlString = "${endpoints.random()}/api/v2/address/$address?details=basic"
        // Go through streamedRequest() so the OkHttp connect/read timeouts
        // apply. mapper.readValue(URL(...)) uses java.net.HttpURLConnection
        // with a default timeout of 0 (infinite) and can hang sync forever.
        val result = streamedRequest(urlString, AccountBasicInfoResponse::class.java)
        // Blockbook's `nonce` is the next nonce to use; `unconfirmedTxs` counts
        // ALL unconfirmed txs at the address (including incoming), so adding it
        // over-counts and produces gaps that leave sent txs stuck in the mempool.
        return result.nonce
    }

    fun getBalance(address: String): BalanceResponse {
        val urlString = "${endpoints.random()}/api/v2/address/$address?details=basic"
        val result = streamedRequest(urlString, AccountBasicInfoResponse::class.java)
        return BalanceResponse(result.balance, result.unconfirmedBalance)
    }

    fun getTransaction(hash: String): Tx {
        val urlString = "${endpoints.random()}/api/v2/tx/$hash"

        return streamedRequest(urlString, Tx::class.java)
    }

    fun getTransactions(
        address: String,
        contractAddress: String? = null,
        knownConfirmedTxids: Set<String> = emptySet(),
    ): List<Tx> {
        return if (contractAddress != null) {
            fetchTransactions(address, contractAddress, knownConfirmedTxids)
                .filter { tx -> tx.getTokenTransfer(contractAddress, address) != null }
        } else {
            fetchTransactions(address, contractAddress, knownConfirmedTxids)
        }
    }

    // Blockbook's /api/v2/address/{addr}?details=txs list silently drops
    // pending txs. The default (no `details`) endpoint returns a `txids` array
    // that does include recent pending ones at the top. Use this to discover
    // hashes we then fetch individually via getTransaction().
    @Throws(IOException::class)
    fun getAddressTxids(address: String): List<String> {
        val url = "${endpoints.random()}/api/v2/address/$address?details=txids&pageSize=50"
        return streamedRequest(url, AddressTxidsResponse::class.java).txids
    }

    fun feeEstimation(block: Int): FeeResult {
        val urlString = "${endpoints.random()}/api/v2/estimatefee/$block"
        return streamedRequest(urlString, FeeResult::class.java)
    }

    /**
     * EIP-1559 fee estimate from the dedicated estimator service (base fee +
     * per-tier cap and tip). Pure fetch — the caller decides whether to cache
     * it via [cacheEip1559Fees]. Throws on network/parse failure so the caller
     * can fall back to the legacy per-block path.
     */
    @Throws(IOException::class)
    fun eip1559FeeEstimation(): Eip1559Snapshot {
        if (eip1559EstimatorUrl.isBlank()) throw IOException("No EIP-1559 estimator configured")
        return streamedRequest(eip1559EstimatorUrl, Eip1559Response::class.java).toSnapshot()
    }

    /** Last cached EIP-1559 snapshot, or null if none/cleared. Non-blocking. */
    fun lastEip1559Fees(): Eip1559Snapshot? = lastEip1559

    fun cacheEip1559Fees(snapshot: Eip1559Snapshot) {
        lastEip1559 = snapshot
    }

    /** Drop the cached snapshot so tip recovery falls back to legacy signing. */
    fun clearEip1559Fees() {
        lastEip1559 = null
    }

    /**
     * Recover the priority fee (tip) matching a chosen maxFeePerGas cap from the
     * last cached snapshot. Picks the tier whose cap is nearest the chosen value
     * (exact for the standard tiers), clamped so the tip is at least 1 wei and
     * never exceeds the available headroom (cap - baseFee). Returns null when no
     * snapshot is cached or the cap leaves no room above the base fee — the
     * caller should then sign legacy.
     */
    fun priorityFeeFor(maxFeePerGas: BigInteger): BigInteger? {
        val snapshot = lastEip1559 ?: return null
        val tier = snapshot.tiers.minByOrNull { (it.maxFeePerGas - maxFeePerGas).abs() } ?: return null
        val headroom = maxFeePerGas - snapshot.baseFeePerGas
        if (headroom < BigInteger.ONE) return null
        return tier.maxPriorityFeePerGas.max(BigInteger.ONE).min(headroom)
    }

    override fun serverListChanged(newEndpoints: Array<HttpEndpoint>) {
        endpoints = newEndpoints.toList()
    }
    class SendResult(val success: Boolean, val message: String?)
}

data class BalanceResponse(val confirmed: BigInteger, val unconfirmed: BigInteger)

private class ApiResponse {
    val blockbook: BlockbookInfo? = null
}

private class AccountBasicInfoResponse {
    val nonce: BigInteger = BigInteger.ZERO
    val unconfirmedTxs: Long = 0
    val balance: BigInteger = BigInteger.ZERO
    val unconfirmedBalance: BigInteger = BigInteger.ZERO
}

private class BlockbookInfo {
    val bestHeight: BigInteger = BigInteger.ZERO
}

private class SendTxResponse {
    val result: String? = null
    val error: String? = null
}

private class Response {
    var transactions: List<Tx> = emptyList()
    val totalPages: Int = 0
}

private class AddressTxidsResponse {
    val txids: List<String> = emptyList()
}

class Tx {
    val txid: String = ""

    @JsonProperty("vin")
    private val vin: List<Vin> = emptyList()

    @JsonProperty("vout")
    private val vout: List<Vin> = emptyList()

    val from: String
        get() = vin[0].addresses!![0]

    val to: String?
        get() = vout[0].addresses?.get(0)

    val blockHeight: BigInteger = BigInteger.ZERO
    val confirmations: BigInteger = BigInteger.ZERO
    val blockTime: Long = 0
    val value: BigInteger = BigInteger.ZERO
    val fees: BigInteger = BigInteger.ZERO
    // the ether that was sent to the user by a contract
    var internalValue: BigInteger? = BigInteger.ZERO

    @JsonProperty("ethereumSpecific")
    private val ethereumSpecific: EthereumSpecific? = null

    val nonce: BigInteger
        get() = ethereumSpecific!!.nonce

    val gasLimit: BigInteger
        get() = ethereumSpecific!!.gasLimit

    val gasUsed: BigInteger?
        get() = ethereumSpecific!!.gasUsed

    /**
     * For a pending type-2 tx blockbook reports the effective price (base fee + tip) as `gasPrice`,
     * which is below the tx's cap: a replacement bumped off it is rejected ("could not replace
     * existing tx"). Until the tx mines, use the cap; once mined the effective price is what was paid.
     */
    val gasPrice: BigInteger
        get() = ethereumSpecific!!.let {
            if (confirmations.signum() == 0 && it.maxFeePerGas.signum() > 0) it.maxFeePerGas else it.gasPrice
        }

    val success: Boolean
        get() = ethereumSpecific!!.status

    /** Why the transaction failed, when the server tells us. Null for successful txs. */
    val failureReason: String?
        get() = ethereumSpecific?.error?.takeIf { it.isNotBlank() }
    val tokenTransfers: List<TokenTransfer> = emptyList()

    fun getTokenTransfer(contractAddress: String, ownerAddress: String): TokenTransfer? {
        val ours = tokenTransfers.filter {
            it.token().equals(contractAddress, true) &&
                    (it.to.equals(ownerAddress, true) || it.from.equals(ownerAddress, true))
        }
        if (ours.isEmpty()) return null
        // Net the transfers rather than adding them up. One transaction can both
        // credit and debit the same token for the same address — a swap, a partial
        // refund, a router hop — and summing them reported the two legs as one
        // oversized transfer in whichever direction the first entry happened to
        // take, which then landed in the balance.
        val net = ours.fold(BigInteger.ZERO) { acc, t ->
            when {
                t.to.equals(ownerAddress, true) && !t.from.equals(ownerAddress, true) -> acc + t.value
                t.from.equals(ownerAddress, true) && !t.to.equals(ownerAddress, true) -> acc - t.value
                else -> acc // self-transfer: no net movement
            }
        }
        val template = ours.first()
        // Direction follows the net amount, not whichever leg happened to be
        // first, and the counterparty is taken from a leg that actually has one —
        // falling back to the owner would make a net-outgoing transfer look like a
        // self-transfer and drop it from the balance entirely.
        return if (net.signum() >= 0) {
            val sender = ours.firstOrNull { !it.from.equals(ownerAddress, true) }?.from ?: ownerAddress
            TokenTransfer(sender, ownerAddress, template.contract, template.token, template.name, net)
        } else {
            val recipient = ours.firstOrNull { !it.to.equals(ownerAddress, true) }?.to ?: ownerAddress
            TokenTransfer(ownerAddress, recipient, template.contract, template.token, template.name, net.negate())
        }
    }

    // For pending txs where tokenTransfers is empty, decode ERC20
    // transfer(address,uint256) from ethereumSpecific.data.
    // Selector 0xa9059cbb = transfer(address,uint256)
    fun getPendingTokenTransfer(contractAddress: String, ownerAddress: String): TokenTransfer? {
        if (tokenTransfers.isNotEmpty()) return getTokenTransfer(contractAddress, ownerAddress)
        // Note: we intentionally don't check `to == contractAddress` here.
        // Blockbook synthesizes vout[0] as the token RECIPIENT for ERC20
        // transfers, not the contract, so that check would always fail for
        // pending ERC20 sends. The caller is responsible for scoping to
        // relevant contracts.
        val data = ethereumSpecific?.data ?: return null
        val hex = data.removePrefix("0x")
        if (hex.length < 136) return null
        val selector = hex.substring(0, 8)
        if (!selector.equals("a9059cbb", true)) return null
        val recipientAddress = "0x" + hex.substring(32, 72)
        val amount = BigInteger(hex.substring(72, 136), 16)
        if (!from.equals(ownerAddress, true) && !recipientAddress.equals(ownerAddress, true)) return null
        return TokenTransfer(from, recipientAddress, contractAddress, "", "", amount)
    }

    override fun toString(): String {
        return """{'txid':$txid,'from':$from,'to':$to,'blockHeight':$blockHeight,'confirmations':$confirmations,
            |'blockTime':$blockTime,'value':$value,'fees':$fees,'nonce':$nonce,'gasLimit':$gasLimit,
            |'gasUsed':$gasUsed,'gasPrice':$gasPrice,${tokenTransfers}}
        """.trimMargin()
    }
}

private class Vin {
    val addresses: List<String>? = emptyList()
}

class TokenTransfer(
    val from: String = "",
    val to: String = "",
    val contract: String = "",
    val token: String = "",
    val name: String = "",
    val value: BigInteger = BigInteger.ZERO
) {

    fun token() = contract.ifEmpty { token }

    override fun toString() = "{'from':$from,'to':$to,'token':${token()},'name':$name,'value':$value}"
}

data class FeeResult(
    val result: BigDecimal = BigDecimal.ZERO
)

// The four fee tiers shown in the UI, mapped to our FeeEstimationsGeneric.
enum class EthFeeLevel { LOW, ECONOMY, NORMAL, HIGH }

/** One tier of an EIP-1559 estimate: the cap and the tip (all in wei). */
data class Eip1559Tier(
    val level: EthFeeLevel,
    val maxFeePerGas: BigInteger,
    val maxPriorityFeePerGas: BigInteger,
)

/** Parsed EIP-1559 estimate: the current base fee plus the per-tier caps/tips. */
data class Eip1559Snapshot(
    val baseFeePerGas: BigInteger,
    val tiers: List<Eip1559Tier>,
) {
    fun tier(level: EthFeeLevel): Eip1559Tier? = tiers.firstOrNull { it.level == level }
}

private val ESTIMATOR_SPEED_TO_LEVEL = mapOf(
    "safe_low" to EthFeeLevel.LOW,
    "average" to EthFeeLevel.ECONOMY,
    "fast" to EthFeeLevel.NORMAL,
    "fastest" to EthFeeLevel.HIGH,
)

private class Eip1559Response {
    @JsonProperty("base_fee_per_gas")
    var baseFeePerGas: BigInteger = BigInteger.ZERO

    @JsonProperty("price_levels")
    var priceLevels: List<Level> = emptyList()

    class Level {
        var speed: String = ""

        @JsonProperty("max_fee_per_gas")
        var maxFeePerGas: BigInteger = BigInteger.ZERO

        @JsonProperty("max_priority_fee_per_gas")
        var maxPriorityFeePerGas: BigInteger = BigInteger.ZERO
    }

    fun toSnapshot(): Eip1559Snapshot {
        val tiers = priceLevels.mapNotNull { level ->
            ESTIMATOR_SPEED_TO_LEVEL[level.speed]?.let {
                Eip1559Tier(it, level.maxFeePerGas, level.maxPriorityFeePerGas)
            }
        }
        return Eip1559Snapshot(baseFeePerGas, tiers)
    }
}

private class EthereumSpecific {
    val nonce: BigInteger = BigInteger.ZERO
    val gasLimit: BigInteger = BigInteger.ZERO
    val gasUsed: BigInteger = BigInteger.ZERO
    val gasPrice: BigInteger = BigInteger.ZERO
    val maxFeePerGas: BigInteger = BigInteger.ZERO
    val data: String? = null
    val status: Boolean = true

    /** Revert reason blockbook reports for a failed tx, e.g. "Out of gas." */
    val error: String? = null
}

interface ServerEthListChangedListener {
    fun serverListChanged(newEndpoints: Array<HttpEndpoint>)
}
