package com.mycelium.wapi.wallet.fio

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.GsonBuilder
import com.mrd.bitlib.util.HexUtils
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import fiofoundation.io.fiosdk.FIOSDK
import fiofoundation.io.fiosdk.errors.serializationprovider.DeserializeTransactionError
import fiofoundation.io.fiosdk.models.fionetworkprovider.ObtDataRecord
import fiofoundation.io.fiosdk.models.fionetworkprovider.RecordObtDataContent
import fiofoundation.io.fiosdk.models.fionetworkprovider.response.GetObtDataResponse
import fiofoundation.io.fiosdk.utilities.HashUtils
import fiofoundation.io.fiosdk.utilities.Utils
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.math.BigDecimal
import java.math.BigInteger
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class FioBlockchainService(private val coinType: CryptoCurrency,
                           private val fioEndpoints: FioEndpoints,
                           private var tpid: String,
                           private val serializationProviderWrapper: IAbiFioSerializationProviderWrapper) : FioTpidChangedListener {
    fun getFioSdk(privkeyString: String) = FIOSDK.getInstance(privkeyString, FIOSDK.derivedPublicKey(privkeyString),
            tpid, serializationProviderWrapper.getAbiFioSerializationProvider(), fioEndpoints.getCurrentApiEndpoint().baseUrl)

    fun getObtData(ownerPublicKey: String, privateKey: String, limit: Int? = null, offset: Int? = null): List<ObtDataRecord> {
        val requestBody = if (limit == null && offset == null) {
            """{"fio_public_key":"$ownerPublicKey"}"""
        } else if (limit == null) {
            """{"fio_public_key":"$ownerPublicKey", "offset":$offset}"""
        } else if (offset == null) {
            """{"fio_public_key":"$ownerPublicKey", "limit":$limit}"""
        } else {
            """{"fio_public_key":"$ownerPublicKey", "limit":$limit, "offset":$offset}"""
        }
        val request = Request.Builder()
                .url(fioEndpoints.getCurrentApiEndpoint().baseUrl + "chain/get_obt_data")
                .post(RequestBody.create("application/json".toMediaType(), requestBody))
                .build()

        val response = client.newCall(request).execute()
        val gson = GsonBuilder().serializeNulls().create()
        val result = gson.fromJson(response.body!!.string(), GetObtDataResponse::class.java)
        for (item in result.records) {
            try {
                val pubkey = if (item.payeeFioPublicKey.equals(ownerPublicKey, true)) item.payerFioPublicKey else item.payeeFioPublicKey
                item.deserializedContent = RecordObtDataContent.deserialize(privateKey, pubkey,
                        serializationProviderWrapper.getAbiFioSerializationProvider(), item.content)
            } catch (deserializationError: DeserializeTransactionError) {
                //eat this error.  We do not want this error to stop the process.
            }
        }

        return result.records
    }

    private fun getActions(accountName: String, pos: BigInteger, offset: BigInteger): List<GetActionsResponse.ActionObject> {
        val requestBody = "{\"account_name\":\"$accountName\", \"pos\":$pos, \"offset\":$offset}"
        val request = Request.Builder()
                .url(fioEndpoints.getCurrentHistoryEndpoint().baseUrl + "history/get_actions")
                .post(RequestBody.create("application/json".toMediaType(), requestBody))
                .build()
        val response = client.newCall(request).execute()
        val result = mapper.readValue(response.body!!.string(), GetActionsResponse::class.java)
        return result.actions
    }

    fun getAccountActionSeqNumber(accountName: String): BigInteger? {
        val actions = getActions(accountName, BigInteger.valueOf(-1), BigInteger.valueOf(-1))
        return actions.firstOrNull()?.accountActionSeq
    }

    fun getTransactions(ownerPublicKey: String, latestBlockNum: BigInteger): List<Tx> {
        val actions: MutableList<GetActionsResponse.ActionObject> = mutableListOf()
        val accountName = Utils.generateActor(ownerPublicKey)

        var result = getActions(accountName, BigInteger.valueOf(-1), BigInteger.valueOf(-1))

        if (result.isNotEmpty()) {
            var pos = result.first().accountActionSeq
            val finish = false
            while (!finish) {
                if (pos < BigInteger.ZERO) {
                    break
                }

                result = getActions(accountName, pos, -offset + BigInteger.ONE)
                actions.addAll(result)
                if (result.isEmpty() || result.size.toBigInteger() < offset) {
                    break
                }
                pos -= offset
            }
            return transform(ownerPublicKey, accountName, actions)
        }
        return emptyList()
    }

    fun getLatestBlock(): BigInteger {
        val request = Request.Builder()
                .url(fioEndpoints.getCurrentApiEndpoint().baseUrl + "chain/get_info")
                .post(RequestBody.create(null, ""))
                .build()
        val response = client.newCall(request).execute()
        val result = mapper.readValue(response.body!!.string(), GetBlockInfoResponse::class.java)
        return result.lastIrreversibleBlockNum.toBigInteger()
    }

    private fun transform(ownerPublicKey: String, accountName: String, txs: MutableList<GetActionsResponse.ActionObject>): List<Tx> {
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
                        getTransferred(ownerPublicKey, accountName, it.act.name, it.act.data, feeMap[it.trxId]
                                ?: Value.zeroValue(coinType)),
                        it.act.data.amount?.let { amount -> Value.valueOf(coinType, amount) } ?: getQuantityValue(it.act.data.quantity!!),
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

    private fun getTransferred(ownerPublicKey: String, accountName: String, type: String, data: GetActionsResponse.ActionObject.ActionTrace.Act.Data, fee: Value): Value {
        when (type) {
            "trnsfiopubky" -> {
                // Check if transaction is sent or received
                if (data.payeePublicKey == ownerPublicKey) {
                    // Check if sending to myself
                    if (data.actor == accountName) {
                        return fee.unaryMinus()
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

    private val offset: BigInteger = BigInteger.valueOf(1000)

    companion object {
        private val mapper = ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        private val client = OkHttpClient()

        @JvmStatic
        fun getPubkeyByActor(fioEndpoints: FioEndpoints, actor: String): String {
            val requestBody = """{"account_name":"$actor"}"""
            val request = Request.Builder()
                    .url(fioEndpoints.getCurrentApiEndpoint().baseUrl + "chain/get_account")
                    .post(RequestBody.create("application/json".toMediaType(), requestBody))
                    .build()
            val response = client.newCall(request).execute()
            val result = mapper.readValue(response.body!!.string(), GetAccountResponse::class.java)
            return result.permissions[0].requiredAuth!!.keys[0].key
        }

        @JvmStatic
        fun getPubkeysByFioName(fioEndpoints: FioEndpoints, fioAddress: String): List<PublicAddressEntry> {
            val md = MessageDigest.getInstance("SHA-1")
            val nameHash = HexUtils.toHex(md.digest(fioAddress.toByteArray()).slice(IntRange(0, 15)).reversed().toByteArray())
            val requestBody = """|{
                    |  "code": "fio.address",
                    |  "scope": "fio.address",
                    |  "table": "fionames",
                    |  "lower_bound": "0x$nameHash",
                    |  "upper_bound": "0x$nameHash",
                    |  "key_type": "i128",
                    |  "index_position": "5",
                    |  "json": true
                    |}""".trimMargin()
            val request = Request.Builder()
                    .url(fioEndpoints.getCurrentApiEndpoint().baseUrl + "chain/get_table_rows")
                    .post(RequestBody.create("application/json".toMediaType(), requestBody))
                    .build()
            val response = client.newCall(request).execute()
            val body = response.body!!.string()
            return mapper.readValue(body, GetPubAddressesResponse::class.java).rows.firstOrNull()?.addresses
                    ?: listOf()
        }

        @JvmStatic
        fun getPubkeyByFioAddress(fioEndpoints: FioEndpoints, fioAddress: String, chainCode: String, tokenCode: String): GetPubAddressResponse {
            val requestBody = """{"fio_address":"$fioAddress","chain_code":"$chainCode","token_code":"$tokenCode"}"""
            val request = Request.Builder()
                    .url(fioEndpoints.getCurrentApiEndpoint().baseUrl + "chain/get_pub_address")
                    .post(RequestBody.create("application/json".toMediaType(), requestBody))
                    .build()
            val response = client.newCall(request).execute()
            return mapper.readValue(response.body!!.string(), GetPubAddressResponse::class.java)
        }

        @JvmStatic
        fun getFeeByEndpoint(fioEndpoints: FioEndpoints, endpointName: String, fioAddress: String = ""): String? {
            val requestBody = """{"end_point":"$endpointName","fio_address":"$fioAddress"}"""
            val request = Request.Builder()
                    .url(fioEndpoints.getCurrentApiEndpoint().baseUrl + "chain/get_fee")
                    .post(RequestBody.create("application/json".toMediaType(), requestBody))
                    .build()

            val response = client.newCall(request).execute()
            val result = mapper.readValue(response.body!!.string(), GetFeeResponse::class.java)
            return result.fee
        }

        @JvmStatic
        fun isFioNameOrDomainAvailable(fioEndpoints: FioEndpoints, fioNameOrDomain: String): Boolean {
            val requestBody = """{"fio_name":"$fioNameOrDomain"}"""
            val request = Request.Builder()
                    .url(fioEndpoints.getCurrentApiEndpoint().baseUrl + "chain/avail_check")
                    .post(RequestBody.create("application/json".toMediaType(), requestBody))
                    .build()
            val response = client.newCall(request).execute()
            val result = mapper.readValue(response.body!!.string(), AvailCheckResponse::class.java)
            return result.isAvailable
        }

        @JvmStatic
        fun getFioNames(fioEndpoints: FioEndpoints, publicKey: String): GetFIONamesResponse? {
            val requestBody = """{"fio_public_key":"$publicKey"}"""
            val request = Request.Builder()
                    .url(fioEndpoints.getCurrentApiEndpoint().baseUrl + "chain/get_fio_names")
                    .post(RequestBody.create("application/json".toMediaType(), requestBody))
                    .build()
            val response = client.newCall(request).execute()
            return mapper.readValue(response.body!!.string(), GetFIONamesResponse::class.java)
        }

        @JvmStatic
        fun getBundledTxsNum(fioEndpoints: FioEndpoints, fioName: String): Int? {
            val hash = getNameHash(fioName)
            val requestBody = """{"code": "fio.address",
                                  "scope": "fio.address",
                                  "table": "fionames",
                                  "lower_bound": "$hash",
                                  "upper_bound": "$hash",
                                  "key_type": "i128",
                                  "index_position": "5",
                                  "json": true}"""
            val request = Request.Builder()
                    .url(fioEndpoints.getCurrentApiEndpoint().baseUrl + "chain/get_table_rows")
                    .post(RequestBody.create("application/json".toMediaType(), requestBody))
                    .build()
            val response = client.newCall(request).execute()
            val result = mapper.readValue(response.body!!.string(), GetFioNamesTableRowsResponse::class.java)
            return result.rows?.firstOrNull { it.name == fioName }?.bundleeligiblecountdown
        }

        // https://developers.fioprotocol.io/api/api-spec/reference/get-table-rows/get-table-rows#compute-index
        private fun getNameHash(fioName: String): String {
            val reverseBytes = HexUtils.toBytes(HashUtils.sha1(fioName).substring(0, 32))
            reverseBytes.reverse()
            return "0x" + HexUtils.toHex(reverseBytes)
        }
    }

    override fun tpidChanged(tpid: String) {
        this.tpid = tpid
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

class GetFioNamesTableRowsResponse {
    val rows: List<Row>? = null

    class Row {
        val name: String? = null
        val bundleeligiblecountdown: Int? = null
    }
}

class GetPubAddressesResponse {
    val rows: List<GetPubAddressesResponseRow> = listOf()
}

class GetPubAddressesResponseRow {
    val addresses: List<PublicAddressEntry> = listOf()
}

class PublicAddressEntry {
    @JsonProperty("token_code")
    val tokenCode: String = ""

    @JsonProperty("chain_code")
    val chainCode: String = ""

    @JsonProperty("public_address")
    val publicAddress: String = ""
}

class GetPubAddressResponse {
    @JsonProperty("public_address")
    val publicAddress: String? = null
    var message: String? = null
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

interface FioTpidChangedListener {
    fun tpidChanged(tpid: String)
}
