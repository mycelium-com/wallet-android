package com.mycelium.wapi.wallet.eth

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.mycelium.net.HttpsEndpoint
import java.io.IOException
import java.math.BigInteger
import java.net.URL
import java.util.logging.Level
import java.util.logging.Logger

abstract class AbstractTransactionService(private val address: String, endpoints: List<HttpsEndpoint>) {
    private val logger = Logger.getLogger(AbstractTransactionService::class.simpleName)
    private val api = "${endpoints.random()}/api/v2/address/"

    @Throws(IOException::class)
    protected fun fetchTransactions(): List<Tx> {
        var urlString = "$api$address?details=txs"
        val mapper = ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        val result: MutableList<Tx> = mutableListOf()

        val initialResponse = mapper.readValue(URL(urlString), Response::class.java)
        result.addAll(initialResponse.transactions)
        for (i in 2..initialResponse.totalPages) {
            logger.log(Level.INFO, "page: $i")
            urlString = "$api$address?details=txs&page=$i"
            val response = mapper.readValue(URL(urlString), Response::class.java)
            result.addAll(response.transactions)
        }
        return result
    }

    abstract fun getTransactions(): List<Tx>
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
        get() = vin[0].addresses[0]

    val to: String
        get() = vout[0].addresses[0]

    val blockHeight: BigInteger = BigInteger.ZERO
    val confirmations: BigInteger = BigInteger.ZERO
    val blockTime: Long = 0
    val value: BigInteger = BigInteger.ZERO
    val fees: BigInteger = BigInteger.ZERO

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
    val addresses: List<String> = emptyList()
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
}