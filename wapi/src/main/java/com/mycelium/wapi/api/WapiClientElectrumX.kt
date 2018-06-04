package com.mycelium.wapi.api

import com.google.common.primitives.UnsignedInteger
import com.google.gson.annotations.SerializedName
import com.megiontechnologies.Bitcoins
import com.mrd.bitlib.StandardTransactionBuilder
import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.model.OutPoint
import com.mrd.bitlib.util.HexUtils
import com.mrd.bitlib.util.Sha256Hash
import com.mycelium.WapiLogger
import com.mycelium.net.ServerEndpoints
import com.mycelium.wapi.api.jsonrpc.*
import com.mycelium.wapi.api.lib.FeeEstimation
import com.mycelium.wapi.api.lib.FeeEstimationMap
import com.mycelium.wapi.api.request.*
import com.mycelium.wapi.api.response.*
import com.mycelium.wapi.model.TransactionOutputEx
import com.mycelium.wapi.model.TransactionStatus
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

/**
 * This is a Wapi Client that avoids calls that require BQS by talking to ElectrumX for related calls
 */
class WapiClientElectrumX(serverEndpoints: ServerEndpoints, logger: WapiLogger, versionCode: String) : WapiClient(serverEndpoints, logger, versionCode) {
    @Volatile
    private lateinit var jsonRpcTcpClient: JsonRpcTcpClient
    @Volatile
    private var bestChainHeight = -1

    private val receiveHeaderCallback = { response: AbstractResponse ->
        bestChainHeight = (response as RpcResponse).getResult(BlockHeader::class.java)!!.height
    }

    init {
        val latch = CountDownLatch(1)
        thread(start = true) {
            jsonRpcTcpClient = JsonRpcTcpClient("electrumx-1.mycelium.com", 50012, logger)
            latch.countDown()
            jsonRpcTcpClient.start()
        }
        if (!latch.await(30, TimeUnit.SECONDS)) {
            throw TimeoutException("JsonRpcTcpClient failed to start within time.")
        }
        logger.logInfo("ElectrumX server version is ${serverFeatures().serverVersion}.")
        jsonRpcTcpClient.writeAsync(HEADRES_SUBSCRIBE_METHOD, RpcParams.listParams(true), receiveHeaderCallback)
    }

    override fun queryUnspentOutputs(request: QueryUnspentOutputsRequest): WapiResponse<QueryUnspentOutputsResponse> {
        val unspent: ArrayList<TransactionOutputEx> = ArrayList()
        val requestsList = ArrayList<RpcRequestOut>()
        val requestsIndexesMap = HashMap<String, Int>()
        val requestAddressesList = ArrayList(request.addresses)
        requestAddressesList.forEach {
            val addrHex = it.toString()
            requestsList.add(RpcRequestOut(LIST_UNSPENT_METHOD, RpcParams.listParams(addrHex)))
        }
        val unspentsArray = jsonRpcTcpClient.write(requestsList, 50000).responses

        //Fill temporary indexes map in order to find right address
        requestsList.forEachIndexed { index, req ->
            requestsIndexesMap[req.id.toString()] = index
        }
        unspentsArray.forEach { response ->
            val outputs = response.getResult(Array<UnspentOutputs>::class.java)
            outputs!!.forEach {
                val script = StandardTransactionBuilder.createOutput(requestAddressesList[requestsIndexesMap[response.id.toString()]!!],
                        it.value, NetworkParameters.testNetwork).script
                unspent.add(TransactionOutputEx(OutPoint(Sha256Hash.fromString(it.txHash), it.txPos), it.height,
                        it.value, script.scriptBytes,
                        script.isCoinBase))
            }
        }

        return WapiResponse(QueryUnspentOutputsResponse(bestChainHeight, unspent))
    }

    override fun queryTransactionInventory(request: QueryTransactionInventoryRequest): WapiResponse<QueryTransactionInventoryResponse> {
//        public final List<Address> addresses;
//        public final int limit;

//        public final int height;
//        public final List<Sha256Hash> txIds;

        @Suppress("UseExpressionBody")
        // TODO: implement
        return super.queryTransactionInventory(request)
    }

    override fun getTransactions(request: GetTransactionsRequest): WapiResponse<GetTransactionsResponse> {
//        public final Collection<Sha256Hash> txIds;

//        public final Collection<TransactionExApi> transactions;

        @Suppress("UseExpressionBody")
        // TODO: implement
        return super.getTransactions(request)
    }

    override fun broadcastTransaction(request: BroadcastTransactionRequest): WapiResponse<BroadcastTransactionResponse> {
        val txHex = HexUtils.toHex(request.rawTransaction)
        val response = jsonRpcTcpClient.write(BROADCAST_METHOD, RpcParams.listParams(txHex), 50000)
        if (response.hasError) {
            logger.logError(response.error?.toString())
            return WapiResponse(BroadcastTransactionResponse(false, null))
        }
        val txId = response.getResult(String::class.java)!!
        return WapiResponse(BroadcastTransactionResponse(true, Sha256Hash.fromString(txId)))
    }

    override fun checkTransactions(request: CheckTransactionsRequest): WapiResponse<CheckTransactionsResponse> {
        val transactionsArray = getTransactions(request.txIds.map { it.toHex() })
        // check for unconfirmed transactions' parents for confirmations and RBF
        val neededParentTxids = transactionsArray.filter { it.confirmations == 0 }.map { it.hash }
        val parentTransactionsArray = getTransactions(neededParentTxids)
        val transactionStatusArray = transactionsArray.map { tx ->
            var unconfirmedChainLength = 0
            var rbfRisk = false
            if (tx.confirmations == 0) {
                // if unconfirmed chain length is one, see if it is two (or more)
                // see if it or parent is RBF
                val parentTransactions = parentTransactionsArray.filter { ptx ->
                    ptx.hash == tx.hash
                }
                rbfRisk = isRbf(tx.vin) || parentTransactions.any { ptx ->
                    isRbf(ptx.vin)
                }
                if (parentTransactions.any { ptx -> ptx.confirmations == 0}) {
                    unconfirmedChainLength = 1
                }
            }
            TransactionStatus(
                    Sha256Hash.fromString(tx.hash),
                    true,
                    if (tx.time == 0) (Date().time / 1000).toInt() else tx.time,
                    if (tx.confirmations > 0) bestChainHeight - tx.confirmations else -1,
                    unconfirmedChainLength, // 0 or 1. we don't dig deeper. 1 == unconfirmed parent
                    rbfRisk)
        }
        return WapiResponse(CheckTransactionsResponse(transactionStatusArray))
    }

    private fun getTransactions(txids: Collection<String>): List<TransactionX> {
        if (txids.isEmpty()) {
            return emptyList()
        }
        val requestsList = txids.map {
            RpcRequestOut(GET_TRANSACTION_METHOD,
                    RpcParams.mapParams(
                            "tx_hash" to it,
                            "verbose" to true))
        }.toList()

        return jsonRpcTcpClient.write(requestsList, 15000).responses.map {
            if (it.hasError) {
                logger.logError("checkTransactions failed: ${it.error}")
            }
            it.getResult(TransactionX::class.java)
        }.filterNotNull()
    }

    private fun isRbf(vin: Array<TransactionInputX>) = vin.any { it.sequence < NON_RBF_SEQUENCE }

    override fun getMinerFeeEstimations(): WapiResponse<MinerFeeEstimationResponse> {
        val blocks: Array<Int> = arrayOf(1, 2, 3, 4, 5, 10, 15, 20) // this is what the wapi server used
        val requestsList = ArrayList<RpcRequestOut>()
        blocks.forEach { nBlocks ->
            requestsList.add(RpcRequestOut(ESTIMATE_FEE_METHOD, RpcParams.listParams(nBlocks)))
        }

        val estimatesArray = jsonRpcTcpClient.write(requestsList, 5000).responses

        val feeEstimationMap = FeeEstimationMap()

        estimatesArray.forEachIndexed { index, response ->
            feeEstimationMap[blocks[index]] = Bitcoins.valueOf(response.getResult(Double::class.java)!!)
        }
        return WapiResponse(MinerFeeEstimationResponse(FeeEstimation(feeEstimationMap, Date())))
    }

    fun serverFeatures(): ServerFeatures {
        val response = jsonRpcTcpClient.write(FEATURES_METHOD, RpcParams.listParams(), 50000)
        return response.getResult(ServerFeatures::class.java)!!
    }

    companion object {
        @Deprecated("Address must be replaced with script")
        private const val LIST_UNSPENT_METHOD = "blockchain.address.listunspent"
        private const val ESTIMATE_FEE_METHOD = "blockchain.estimatefee"
        private const val BROADCAST_METHOD = "blockchain.transaction.broadcast"
        private const val GET_TRANSACTION_METHOD = "blockchain.transaction.get"
        private const val FEATURES_METHOD = "server.features"
        private const val HEADRES_SUBSCRIBE_METHOD = "blockchain.headers.subscribe"
        private val NON_RBF_SEQUENCE = UnsignedInteger.MAX_VALUE.toLong()
    }
}

data class ServerFeatures(
        @SerializedName("server_version") val serverVersion: String
)

data class TransactionX(
        val hash: String,
        val blockhash: String,
        val blocktime: Long,
        val confirmations: Int,
        val time: Int,
        val vin: Array<TransactionInputX>,
        var rbfRisk: Boolean
)

data class TransactionInputX(
        val txid: String,
        val sequence: Long
)

data class UnspentOutputs(
        @SerializedName("tx_hash") val txHash: String,
        @SerializedName("tx_pos") val txPos: Int,
        @SerializedName("height") val height: Int,
        @SerializedName("value") val value: Long
)

data class BlockHeader(
        @SerializedName("height") val height: Int,
        @SerializedName("hex") val hex: String
)