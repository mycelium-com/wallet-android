package com.mycelium.wapi.wallet.eth

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.mycelium.net.HttpEndpoint
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.IOException
import java.math.BigInteger
import java.net.URL

class EthBlockchainService(private var endpoints: List<HttpEndpoint>)
    : ServerEthListChangedListener {
    private val mapper = ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

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

        val result = mapper.readValue(response.body()!!.string(), SendTxResponse::class.java)
        return SendResult(result.result != null, result.error)
    }

    fun getBlockHeight(): BigInteger {
        val urlString = "${endpoints.random()}/api/"

        return mapper.readValue(URL(urlString), ApiResponse::class.java).blockbook!!.bestHeight
    }

    fun getNonce(address: String): BigInteger {
        val urlString = "${endpoints.random()}/api/v2/address/$address?details=basic"
        val result = mapper.readValue(URL(urlString), AccountBasicInfoResponse::class.java)
        return result.nonce + BigInteger.valueOf(result.unconfirmedTxs)
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
            fetchTransactions(address).filter { tx -> tx.getTokenTransfer(contractAddress, address) != null }
        } else {
            fetchTransactions(address)
        }
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
            tokenTransfers.find { it.token().equals(contractAddress, true) &&
                    (it.to.equals(ownerAddress, true) || it.from.equals(ownerAddress, true))
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

class TokenTransfer {
    val from: String = ""
    val to: String = ""
    val contract: String = ""
    val token: String = ""
    val name: String = ""
    val value: BigInteger = BigInteger.ZERO

    fun token() = contract.ifEmpty { token }

    override fun toString() = "{'from':$from,'to':$to,'token':${token()},'name':$name,'value':$value}"
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
