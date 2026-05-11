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
import java.net.URL
import java.util.concurrent.TimeUnit

class EthBlockchainService(private var endpoints: List<HttpEndpoint>)
    : ServerEthListChangedListener {
    private val mapper = ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    private fun client(urlString: String): String {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder().url(urlString).build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw IOException("Unexpected code $response")
        val body = response.body?.string() ?: throw IOException("Empty body")
        return body
    }

    // Stream-parse to avoid OOM on accounts with large tx histories (loading
    // the full body as a String can require 100+ MB before parsing starts).
    private fun <T> streamedRequest(urlString: String, type: Class<T>): T {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        val request = Request.Builder().url(urlString).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            response.close()
            throw IOException("Unexpected code $response")
        }
        val body = response.body ?: throw IOException("Empty body")
        return body.use { mapper.readValue(it.byteStream(), type) }
    }

    @Throws(IOException::class)
    private fun fetchTransactions(address: String, contractAddress: String? = null): List<Tx> {
        // Blockbook has a bug with `&contract=...` filtering — server returns 0 txs/balance.
        // Fetch all address txs and rely on client-side filtering in getTransactions().
        // val contractAddressSegment = if (contractAddress != null) "&contract=$contractAddress" else ""
        // Cap page size so a single page response is always bounded. Smaller
        // pages also let us stop early when we only need recent txs.
        val pageSize = 200
        var urlString = "${endpoints.random()}/api/v2/address/$address?details=txs&pageSize=$pageSize" //+ contractAddressSegment

        val result: MutableList<Tx> = mutableListOf()

        val initialResponse = streamedRequest(urlString, Response::class.java)
        result.addAll(initialResponse.transactions)
        for (i in 2..initialResponse.totalPages) {
            urlString = "${endpoints.random()}/api/v2/address/$address?details=txs&pageSize=$pageSize&page=$i" //+ contractAddressSegment
            val response = streamedRequest(urlString, Response::class.java)
            result.addAll(response.transactions)
        }
        return result
    }

    fun sendTransaction(hex: String): SendResult {
        val client = OkHttpClient()
        val url = URL("${endpoints.random()}/api/v2/sendtx/")
        val request = Request.Builder()
                .url(url)
                .post(RequestBody.create(null, hex))
                .build()
        val response = client.newCall(request).execute()

        val result = mapper.readValue(response.body!!.string(), SendTxResponse::class.java)
        return SendResult(result.result != null, result.error)
    }

    fun getBlockHeight(): BigInteger {
        val urlString = "${endpoints.random()}/api/"

        return mapper.readValue(client(urlString), ApiResponse::class.java).blockbook!!.bestHeight
    }

    fun getNonce(address: String): BigInteger {
        val urlString = "${endpoints.random()}/api/v2/address/$address?details=basic"
        val result = mapper.readValue(URL(urlString), AccountBasicInfoResponse::class.java)
        // Blockbook's `nonce` is the next nonce to use; `unconfirmedTxs` counts
        // ALL unconfirmed txs at the address (including incoming), so adding it
        // over-counts and produces gaps that leave sent txs stuck in the mempool.
        return result.nonce
    }

    fun getBalance(address: String): BalanceResponse {
        val urlString = "${endpoints.random()}/api/v2/address/$address?details=basic"
        val result = mapper.readValue(URL(urlString), AccountBasicInfoResponse::class.java)
        return BalanceResponse(result.balance, result.unconfirmedBalance)
    }

    fun getTransaction(hash: String): Tx {
        val urlString = "${endpoints.random()}/api/v2/tx/$hash"

        return mapper.readValue(URL(urlString), Tx::class.java)
    }

    fun getTransactions(address: String, contractAddress: String? = null): List<Tx> {
        return if (contractAddress != null) {
            fetchTransactions(address, contractAddress).filter { tx -> tx.getTokenTransfer(contractAddress, address) != null }
        } else {
            fetchTransactions(address, contractAddress)
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
        return mapper.readValue(URL(urlString), FeeResult::class.java)
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

    val gasPrice: BigInteger
        get() = ethereumSpecific!!.gasPrice

    val success: Boolean
        get() = ethereumSpecific!!.status
    val tokenTransfers: List<TokenTransfer> = emptyList()

    fun getTokenTransfer(contractAddress: String, ownerAddress: String): TokenTransfer? =
            tokenTransfers.filter { it.token().equals(contractAddress, true) &&
                    (it.to.equals(ownerAddress, true) || it.from.equals(ownerAddress, true))
            }.let { list ->
                if (list.isNotEmpty()) {
                    var sum = BigInteger.ZERO
                    list.forEach { sum = sum.plus(it.value) }
                    list.first().let {
                        TokenTransfer(it.from, it.to, it.contract, it.token, it.name, sum)
                    }
                } else {
                    null
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

private class EthereumSpecific {
    val nonce: BigInteger = BigInteger.ZERO
    val gasLimit: BigInteger = BigInteger.ZERO
    val gasUsed: BigInteger = BigInteger.ZERO
    val gasPrice: BigInteger = BigInteger.ZERO
    val data: String? = null
    val status: Boolean = true
}

interface ServerEthListChangedListener {
    fun serverListChanged(newEndpoints: Array<HttpEndpoint>)
}
