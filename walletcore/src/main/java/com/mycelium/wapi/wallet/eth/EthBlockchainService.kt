package com.mycelium.wapi.wallet.eth

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.net.HttpEndpoint
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.IOException
import java.math.BigInteger
import java.net.URL

class EthBlockchainService(private var endpoints: List<HttpEndpoint>,
                           networkParameters: NetworkParameters)
    : ServerEthListChangedListener {
    private val mapper = ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    private val etherscanApiUrl = if (networkParameters.isProdnet) "https://api.etherscan.io" else
        "https://api-ropsten.etherscan.io"
    private val OFFSET = 10000

    @Throws(IOException::class)
    private fun fetchTransactions(address: String): List<Tx> {
        var urlString = "${endpoints.random()}/api/v2/address/$address?details=txs"
        val result: MutableList<Tx> = mutableListOf()

        val initialResponse = mapper.readValue(URL(urlString), Response::class.java)
        result.addAll(initialResponse.transactions)
        for (i in 2..initialResponse.totalPages) {
            urlString = "${endpoints.random()}/api/v2/address/$address?details=txs&page=$i"
            val response = mapper.readValue(URL(urlString), Response::class.java)
            result.addAll(response.transactions)
        }
        calcInternalValue(address, result)
        return result
    }

    // internal value is the ether that was sent to the user by a contract
    private fun calcInternalValue(address: String, result: MutableList<Tx>) {
        try {
            val intTxs: MutableList<EtherscanInternalTransactions.InternalTransaction> = mutableListOf()
            var i = 1
            var urlString = "$etherscanApiUrl/api?module=account&action=txlistinternal&address=$address&startblock=0&endblock=99999999&page=$i&offset=$OFFSET&sort=asc&apikey=KWQPBBFJQYAT5P447MM8322R5BVY8C2MG2"
            var response = mapper.readValue(URL(urlString), EtherscanInternalTransactions::class.java)
            intTxs.addAll(response.result)
            while (response.result.size == OFFSET) {
                i++
                urlString = "$etherscanApiUrl/api?module=account&action=txlistinternal&address=$address&startblock=0&endblock=99999999&page=$i&offset=$OFFSET&sort=asc&apikey=KWQPBBFJQYAT5P447MM8322R5BVY8C2MG2"
                response = mapper.readValue(URL(urlString), EtherscanInternalTransactions::class.java)
                intTxs.addAll(response.result)
            }

            val txsToAdd = intTxs.filterNot { intTx -> result.map { it.txid }.contains(intTx.hash) }
            result.addAll(txsToAdd.map {
                getTransaction(it.hash)
            })

            // maps hash to [value of transferred eth within tx with the hash]
            val mapp = intTxs.map { tx ->
                tx.hash to intTxs.filter {
                    it.hash == tx.hash && it.to.equals(address, true)
                }.map { it.value }.fold(BigInteger.ZERO, BigInteger::add)
            }.toMap()
            result.forEach { tx ->
                tx.internalValue = mapp[tx.txid]
            }
        } catch (ignore: Exception) {
        }
    }

    fun sendTransaction(hex: String): SendResult {
        val client = OkHttpClient()
        val url = URL("${endpoints.random()}/api/v2/sendtx/")
        val request = Request.Builder()
                .url(url)
                .post(RequestBody.create(null, hex))
                .build()
        val response = client.newCall(request).execute()

        val result = mapper.readValue(response.body()!!.string(), SendTxResponse::class.java)
        return SendResult(result.result != null, result.error)
    }

    fun getBlockHeight(): BigInteger {
        val urlString = "${endpoints.random()}/api/"

        return mapper.readValue(URL(urlString), ApiResponse::class.java).blockbook!!.bestHeight
    }

    fun getNonce(address: String): BigInteger {
        val urlString = "${endpoints.random()}/api/v2/address/$address?details=basic"
        val result = mapper.readValue(URL(urlString), NonceResponse::class.java)
        return result.nonce + BigInteger.valueOf(result.unconfirmedTxs)
    }

    fun getTransaction(hash: String): Tx {
        val urlString = "${endpoints.random()}/api/v2/tx/$hash"

        return mapper.readValue(URL(urlString), Tx::class.java)
    }

    fun getTransactions(address: String, contractAddress: String? = null): List<Tx> {
        return if (contractAddress != null) {
            fetchTransactions(address).filter { tx -> tx.getTokenTransfer(contractAddress) != null }
        } else {
            fetchTransactions(address)
                    .filter {
                        it.tokenTransfers.isEmpty() ||
                                (it.tokenTransfers.isNotEmpty() && it.tokenTransfers.any { transfer ->
                                    isOutgoing(address, transfer)
                                })
                    }
        }
    }

    override fun serverListChanged(newEndpoints: Array<HttpEndpoint>) {
        endpoints = newEndpoints.toList()
    }
    class SendResult(val success: Boolean, val message: String?)
}

private fun isOutgoing(address: String, transfer: TokenTransfer) =
        transfer.from.equals(address, true) && !transfer.to.equals(address, true) ||
                transfer.from.equals(address, true) && transfer.to.equals(address, true)

private class ApiResponse {
    val blockbook: BlockbookInfo? = null
}

private class NonceResponse {
    val nonce: BigInteger = BigInteger.ZERO
    val unconfirmedTxs: Long = 0
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

private class EtherscanInternalTransactions {
    val status: Int = 0
    val result: List<InternalTransaction> = emptyList()

    class InternalTransaction {
        val hash: String = ""
        val to: String = ""
        val value: BigInteger = BigInteger.ZERO
    }
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

    fun getTokenTransfer(contractAddress: String): TokenTransfer? =
            tokenTransfers.find { it.token.equals(contractAddress, true) }

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

class TokenTransfer {
    val from: String = ""
    val to: String = ""
    val token: String = ""
    val name: String = ""
    val value: BigInteger = BigInteger.ZERO

    override fun toString() = "{'from':$from,'to':$to,'token':$token,'name':$name,'value':$value}"
}

private class EthereumSpecific {
    val nonce: BigInteger = BigInteger.ZERO
    val gasLimit: BigInteger = BigInteger.ZERO
    val gasUsed: BigInteger = BigInteger.ZERO
    val gasPrice: BigInteger = BigInteger.ZERO
    val status: Boolean = true
}

interface ServerEthListChangedListener {
    fun serverListChanged(newEndpoints: Array<HttpEndpoint>)
}
