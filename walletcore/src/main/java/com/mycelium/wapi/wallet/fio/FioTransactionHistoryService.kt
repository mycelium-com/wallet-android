package com.mycelium.wapi.wallet.fio

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.fio.coins.FIOTest
import com.mycelium.wapi.wallet.fio.coins.FIOToken
import fiofoundation.io.fiosdk.utilities.Utils
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.math.BigDecimal
import java.math.BigInteger
import java.text.SimpleDateFormat
import java.util.*

class FioTransactionHistoryService(private val coinType: CryptoCurrency, private val ownerPublicKey: String, private val accountName: String) {
    private val mapper = ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    var lastActionSequenceNumber: BigInteger = BigInteger.ZERO

    fun getTransactions(latestBlockNum: BigInteger): List<Tx> {
        val actions: MutableList<GetActionsResponse.ActionObject> = mutableListOf()
        val client = OkHttpClient()
        var requestBody = "{\"account_name\":\"$accountName\", \"pos\":-1, \"offset\":-1}"
        var request = Request.Builder()
                .url(url)
                .post(RequestBody.create(MediaType.parse("application/json"), requestBody))
                .build()
        try {
            var response = client.newCall(request).execute()
            var result = mapper.readValue(response.body()!!.string(), GetActionsResponse::class.java)

            if (result.actions.isNotEmpty()) {
                lastActionSequenceNumber = result.actions[0].accountActionSeq
                var pos = lastActionSequenceNumber
                val finish = false
                while (!finish) {
                    if (pos < BigInteger.ZERO) {
                        break
                    }

                    requestBody = "{\"account_name\":\"$accountName\", \"pos\":$pos, \"offset\":${-offset + BigInteger.ONE}}"
                    request = Request.Builder()
                            .url(url)
                            .post(RequestBody.create(MediaType.parse("application/json"), requestBody))
                            .build()
                    try {
                        response = client.newCall(request).execute()
                        result = mapper.readValue(response.body()!!.string(), GetActionsResponse::class.java)
                        actions.addAll(result.actions)

                        if (result.actions.isEmpty() || result.actions.size.toBigInteger() < offset) {
                            break
                        }
                        pos -= offset
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                return transform(actions)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return emptyList()
    }

    fun getLatestBlock(): BigInteger? {
        val client = OkHttpClient()
        val request = Request.Builder()
                .url((coinType as FIOToken).url + "chain/get_info")
                .post(RequestBody.create(null, ""))
                .build()
        return try {
            val response = client.newCall(request).execute()
            val result = mapper.readValue(response.body()!!.string(), GetBlockInfoResponse::class.java)
            result.lastIrreversibleBlockNum.toBigInteger()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun transform(txs: MutableList<GetActionsResponse.ActionObject>): List<Tx> {
        val result: MutableList<Tx> = mutableListOf()
        val feeMap: Map<String, Value> = txs
                .filter { isFee(it.actionTrace!!.act!!.name, it.actionTrace.act!!.data!!.to) }
                .map { it.actionTrace!!.trxId to getQuantityValue(it.actionTrace.act!!.data!!.quantity!!) }.toMap()
        txs.map { it.actionTrace!! }.forEach {
            val type = it.act!!.name
            if (isTransferToPubKey(type) ||
                    isTransferButNotFee(type, it.act.data!!.to)) {
                val sender = if (isTransferToPubKey(type)) {
                    it.act.data!!.actor!!
                } else {
                    it.act.data!!.from!!
                }
                val receiver = if (isTransferToPubKey(type)) {
                    Utils.generateActor(it.act.data.payeePublicKey!!)
                } else {
                    it.act.data.to!!
                }

                result.add(Tx(it.trxId,
                        sender,
                        receiver,
                        it.act.data.memo ?: "",
                        if (it.blockNum > BigInteger.ZERO) it.blockNum else BigInteger.ZERO,
                        getTimestamp(it.blockTime),
                        getTransferred(it.act.name, it.act.data, feeMap[it.trxId]
                                ?: Value.zeroValue(coinType)),
                        Value.valueOf(coinType, it.act.data.amount!!),
                        feeMap[it.trxId] ?: Value.zeroValue(coinType)
                ))
            }
        }
        return result
    }

    private fun isTransferToPubKey(type: String) =
            type == "trnsfiopubky"

    private fun isTransferButNotFee(type: String, receiver: String?) =
            type == "transfer" && receiver != "fio.treasury"

    private fun isFee(type: String, receiver: String?) =
            type == "transfer" && receiver == "fio.treasury"

    private fun getTimestamp(timeString: String): Long {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("GMT")
        return sdf.parse(timeString).time / 1000
    }

    private fun getTransferred(type: String, data: GetActionsResponse.ActionObject.ActionTrace.Act.Data, fee: Value): Value {
        when (type) {
            "trnsfiopubky" -> {
                // Check if transaction is sent or received
                if (data.payeePublicKey == ownerPublicKey) {
                    // Check if sending to myself
                    if (data.actor == accountName) {
                        return Value.zeroValue(coinType)
                    }
                    return Value.valueOf(coinType, data.amount!!)
                } else {
                    return (Value.valueOf(coinType, data.amount!!) + fee).unaryMinus()
                }
            }
            "transfer" -> {
                // Check if fee paid or received
                return if (data.to == accountName) {
                    getQuantityValue(data.quantity!!)
                } else {
                    (getQuantityValue(data.quantity!!) + fee).unaryMinus()
                }
            }
            else -> throw IllegalStateException("I don't know such type $type")
        }
    }

    private fun getQuantityValue(quantity: String): Value {
        // `quantity` is such string - `2.000000000 FIO`
        val amount = quantity.split(' ')
        return Value.valueOf(coinType, BigDecimal(amount[0])
                .multiply(BigDecimal.TEN.pow(coinType.unitExponent)).toBigIntegerExact())
    }

    val url = if (coinType is FIOTest) "https://fiotestnet.greymass.com/v1/history/get_actions" else
        "https://fio.greymass.com/v1/history/get_actions"
    val offset = BigInteger.valueOf(20)

    companion object {
        @JvmStatic
        fun getPubkeyByActor(actor: String, coinType: CryptoCurrency): String? {
            val mapper = ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            val client = OkHttpClient()
            val requestBody = """{"account_name":"$actor"}"""
            val request = Request.Builder()
                    .url((coinType as FIOToken).url + "chain/get_account")
                    .post(RequestBody.create(MediaType.parse("application/json"), requestBody))
                    .build()
            return try {
                val response = client.newCall(request).execute()
                val result = mapper.readValue(response.body()!!.string(), GetAccountResponse::class.java)
                result.permissions[0].requiredAuth!!.keys[0].key
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        @JvmStatic
        fun getPubkeyByFioAddress(fioAddress: String, coinType: CryptoCurrency): String? {
            val mapper = ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            val client = OkHttpClient()
            val requestBody = """{"fio_address":"$fioAddress","chain_code":"FIO","token_code":"FIO"}"""
            val request = Request.Builder()
                    .url((coinType as FIOToken).url + "chain/get_pub_address")
                    .post(RequestBody.create(MediaType.parse("application/json"), requestBody))
                    .build()
            return try {
                val response = client.newCall(request).execute()
                val result = mapper.readValue(response.body()!!.string(), GetPubAddressResponse::class.java)
                result.publicAddress
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        @JvmStatic
        fun getFeeByEndpoint(fioToken: FIOToken, endpoint: String, fioAddress: String = ""): String? {
            val mapper = ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            val client = OkHttpClient()
            val requestBody = """{"end_point":"$endpoint","fio_address":"$fioAddress"}"""
            val request = Request.Builder()
                    .url(fioToken.url + "chain/get_fee")
                    .post(RequestBody.create(MediaType.parse("application/json"), requestBody))
                    .build()
            return try {
                val response = client.newCall(request).execute()
                val result = mapper.readValue(response.body()!!.string(), GetFeeResponse::class.java)
                result.fee
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        @JvmStatic
        fun isFioNameOrDomainAvailable(fioToken: FIOToken, fioNameOrDomain: String): Boolean? {
            val mapper = ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            val client = OkHttpClient()
            val requestBody = """{"fio_name":"$fioNameOrDomain"}"""
            val request = Request.Builder()
                    .url(fioToken.url + "chain/avail_check")
                    .post(RequestBody.create(MediaType.parse("application/json"), requestBody))
                    .build()
            return try {
                val response = client.newCall(request).execute()
                val result = mapper.readValue(response.body()!!.string(), AvailCheckResponse::class.java)
                result.isAvailable
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        @JvmStatic
        fun getFioNames(fioToken: FIOToken, publicKey: String): GetFIONamesResponse? {
            val mapper = ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            val client = OkHttpClient()
            val requestBody = """{"fio_public_key":"$publicKey"}"""
            val request = Request.Builder()
                    .url(fioToken.url + "chain/get_fio_names")
                    .post(RequestBody.create(MediaType.parse("application/json"), requestBody))
                    .build()
            return try {
                val response = client.newCall(request).execute()
                val result = mapper.readValue(response.body()!!.string(), GetFIONamesResponse::class.java)
                result
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}

data class Tx(val txid: String, val fromAddress: String, val toAddress: String, val memo: String,
              val blockNumber: BigInteger, val timestamp: Long, val transferred: Value,
              val sum: Value, val fee: Value) {

    override fun toString(): String {
        return """{'txid':$txid, 'fromAddress':$fromAddress,'toAddress':$toAddress,'memo':$memo,
            'blockNumber':$blockNumber,'timestamp':$timestamp,'value':$sum,'fee':$fee}
        """.trimMargin()
    }
}

class GetFIONamesResponse {
    val fio_domains: List<FioDomain>? = null
    val fio_addresses: List<FioName>? = null
    val message: String? = null

    class FioDomain {
        val fio_domain: String = ""
        val expiration: String = ""

        @JsonProperty("is_public")
        val isPublic: Int = 0
    }

    class FioName {
        val fio_address: String = ""
        val expiration: String = ""
    }
}

class GetAccountResponse {
    val permissions: List<Permission> = emptyList()

    class Permission {
        @JsonProperty("required_auth")
        val requiredAuth: RequiredAuth? = null

        class RequiredAuth {
            var keys: List<Key> = emptyList()

            class Key {
                val key: String = ""
            }
        }
    }
}

class GetPubAddressResponse {
    @JsonProperty("public_address")
    val publicAddress: String? = null
}

class GetFeeResponse {
    val fee: String? = null
}

class AvailCheckResponse {
    //1 - FIO Address or Domain is registered
    //0 - FIO Address or Domain is not registered
    @JsonProperty("is_registered")
    private val isRegistered1: String? = null
    val isAvailable: Boolean
        get() = isRegistered1!! != "1"
}

class GetBlockInfoResponse {
    @JsonProperty("last_irreversible_block_num")
    val lastIrreversibleBlockNum: String = ""
}

class GetActionsResponse {
    val actions: List<ActionObject> = emptyList()

    class ActionObject {
        @JsonProperty("account_action_seq")
        val accountActionSeq: BigInteger = BigInteger.ZERO

        @JsonProperty("action_trace")
        val actionTrace: ActionTrace? = null

        class ActionTrace {
            val receiver: String = ""

            val act: Act? = null

            @JsonProperty("trx_id")
            val trxId: String = ""

            @JsonProperty("block_num")
            val blockNum: BigInteger = BigInteger.ZERO

            @JsonProperty("block_time")
            val blockTime: String = ""

            class Act {
                val account: String = ""
                val name: String = ""
                val data: Data? = null

                @JsonProperty("hex_data")
                val hexData: String = ""

                class Data {
                    @JsonProperty("payee_public_key")
                    val payeePublicKey: String? = null
                    val amount: Long? = null

                    @JsonProperty("max_fee")
                    val maxFee: Long? = null
                    val actor: String? = null
                    val tpid: String? = null
                    val quantity: String? = null
                    val memo: String? = null
                    val to: String? = null
                    val from: String? = null
                }
            }
        }

        override fun toString(): String {
            return """{'accountActionSeq':$accountActionSeq}
        """.trimMargin()
        }
    }
}
