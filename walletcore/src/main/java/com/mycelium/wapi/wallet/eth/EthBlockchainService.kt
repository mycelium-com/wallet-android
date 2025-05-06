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
import java.util.concurrent.TimeUnit

class EthBlockchainService(private var endpoints: List<HttpEndpoint>)
    : ServerEthListChangedListener {
    private val mapper = ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    private fun client(urlString: String): String {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url(urlString)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
            .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36")
            .header("Cookie", "1.2.1.1-LL1qWmhY.0qWKFykqLT7i.DX4iccL.JgAPm0jgaxV_xn_wBBLRb5ZLWI1Yt4C608nqjNktlSMKO61H.6gNv0ge5wAjS8LE9pXqxymaMu.35DZaBMJ5KzDKNIeiWK883dwOQ.mO7PA1B3HC3eBMqSHQtycN4GFP7lab_KwfRDbzigYB8N4A0.Yun5XwC7I7lIhfd_NGFkTz_SMmUDlQn8yrtleZIzYwQtg_bafyWkHsCCeOGhOegcznjn1rpcqEDFlUkIP.BlE0xQZIDPmtNQTQpL391rtSGJDf5JMTiTSCZ1GRDswkLnpTaoSfoc3MBYC5dySfeeA7nR04HVU_8Awg; secondary_coin=USD=false; _gcl_au=1.1.119604374.1746113132; _ga_34JWL0HY2X=GS1.1.1746113132.1.0.1746113132.0.0.1140827049; _ga=GA1.1.1989444470.1746113132; FPID=FPID2.2.qIJg2M10tpoHtla%2FlWDjKc7SZu%2BmkWnuk5NaBSkDgvQ%3D.1746113132; FPLC=zvt2ud4j%2BNwn9cU5EmCpf7JWHIGEcGDyViNL8z5NkMV6LklcRVZX70poOTqBNwSh%2BHkO9Kq%2F%2BgLJjN80g9A4G7hCZI0nm3KrmOM4ABisl0g4TP0HNfm%2Fy%2B3jkhe1Sw%3D%3D; FPAU=1.1.119604374.1746113132")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw IOException("Unexpected code $response")
        val body = response.body?.string() ?: throw IOException("Empty body")
        return body
    }

    @Throws(IOException::class)
    private fun fetchTransactions(address: String, contractAddress: String? = null): List<Tx> {
        val contractAddressSegment = if(contractAddress != null) "&contract=$contractAddress" else ""
        var urlString = "${endpoints.random()}/api/v2/address/$address?details=txs" + contractAddressSegment

        val result: MutableList<Tx> = mutableListOf()

        val initialResponse = mapper.readValue(client(urlString), Response::class.java)
        result.addAll(initialResponse.transactions)
        for (i in 2..initialResponse.totalPages) {
            urlString = "${endpoints.random()}/api/v2/address/$address?details=txs&page=$i" + contractAddressSegment
            val response = mapper.readValue(client(urlString), Response::class.java)
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
            fetchTransactions(address, contractAddress).filter { tx -> tx.getTokenTransfer(contractAddress, address) != null }
        } else {
            fetchTransactions(address, contractAddress)
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
