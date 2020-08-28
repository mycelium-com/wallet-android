package com.mycelium.wapi.wallet.fio

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.lang.IllegalStateException
import java.math.BigDecimal
import java.math.BigInteger
import java.text.SimpleDateFormat
import java.util.*

class FioTransactionHistoryService(private val coinType: CryptoCurrency, private val ownerPublicKey: String, private val accountName: String) {
    private val mapper = ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    fun fetchTransactions(latestBlockNum: BigInteger): List<Tx> {
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
                // get latest sec
                var pos = result.actions[0].accountActionSeq
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
                val txs = transform(actions)
                return txs
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return emptyList()
    }

    fun getLatestBlock(): BigInteger? {
        val client = OkHttpClient()
        val request = Request.Builder()
                .url("http://testnet.fioprotocol.io/v1/chain/get_info")
                .post(RequestBody.create(null, ""))
                .build()
        return try {
            val response = client.newCall(request).execute()
            val result = mapper.readValue(response.body()!!.string(), GetBlockInfoResponse::class.java)
            result.last_irreversible_block_num.toBigInteger()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun transform(txs: MutableList<GetActionsResponse.ActionObject>): List<Tx> {
        val result: MutableList<Tx> = mutableListOf()
        val feeMap: Map<String, Value> = txs
                .filter { it.actionTrace!!.act!!.name == "transfer" && it.actionTrace!!.act!!.data!!.to == "fio.treasury" }
                .map { it.actionTrace!!.trxId to getQuantityValue(it.actionTrace.act!!.data!!.quantity!!) }.toMap()
        txs.map { it.actionTrace!! }.forEach {
            if (it.act!!.name == "trnsfiopubky") {
                result.add(Tx(it.trxId,
                        it.act!!.data!!.actor!!, // sender actor
                        it.act.data!!.payee_public_key!!, // receiver
                        it.act.data.memo ?: "",
                        if (it.blockNum > BigInteger.ZERO) it.blockNum else BigInteger.ZERO,
                        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.mmm", Locale.US).parse(it.blockTime).time / 1000,
                        getAmount(it.act.name, it.act.data, feeMap[it.trxId] ?: Value.zeroValue(coinType)),
                        feeMap[it.trxId] ?: Value.zeroValue(coinType)
                ))
            } else if (it.act!!.name == "transfer" && it.act!!.data!!.to != "fio.treasury") {
                result.add(Tx(it.trxId,
                        it.act.data!!.actor!!,
                        it.act.data.to!!,
                        it.act.data.memo ?: "",
                        if (it.blockNum > BigInteger.ZERO) it.blockNum else BigInteger.ZERO,
                        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.mmm", Locale.US).parse(it.blockTime).time / 1000,
                        getAmount(it.act.name, it.act.data, feeMap[it.trxId] ?: Value.zeroValue(coinType)),
                        feeMap[it.trxId] ?: Value.zeroValue(coinType)))
            }
        }
        return result
    }

    private fun getAmount(type: String, data: GetActionsResponse.ActionObject.ActionTrace.Act.Data, fee: Value): Value {
        when (type) {
            "trnsfiopubky" -> {
                // Check if transaction is sent or received
                if (data.payee_public_key == ownerPublicKey) {
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

    val url = "https://fiotestnet.greymass.com/v1/history/get_actions"
    val offset = BigInteger.valueOf(20)
}

data class Tx(val txid: String, val fromAddress: String, val toAddress: String, val memo: String,
              val blockNumber: BigInteger, val timestamp: Long,
              val value: Value, val fee: Value) {

    override fun toString(): String {
        return """{'txid':$txid, 'fromAddress':$fromAddress,'toAddress':$toAddress,'memo':$memo,
            'blockNumber':$blockNumber,'timestamp':$timestamp,'value':$value,'fee':$fee}
        """.trimMargin()
    }
}
class GetBlockInfoResponse {
    val last_irreversible_block_num: String = ""
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
                    val payee_public_key: String? = null
                    val amount: Long? = null
                    val max_fee: Long? = null
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
