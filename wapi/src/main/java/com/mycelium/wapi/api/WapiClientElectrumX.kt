package com.mycelium.wapi.api

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

    private val receiveHeaderCallback = { responce: AbstractResponse -> bestChainHeight = (responce as RpcResponse).getResult(BlockHeader::class.java)!!.height }

    init {
        val latch = CountDownLatch(1)
        thread(start = true) {
            jsonRpcTcpClient = JsonRpcTcpClient("electrumx-1.mycelium.com", 50012, logger)
            latch.countDown()
            jsonRpcTcpClient.start()
        }
        if (!latch.await(10, TimeUnit.SECONDS)) {
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
        requestsList.forEachIndexed { index, request ->
            requestsIndexesMap[request.id.toString()] = index
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
        val txIds: ArrayList<Sha256Hash> = ArrayList()
        val requestsList = ArrayList<RpcRequestOut>(request.addresses.size)
        request.addresses.forEach {
            val addrHex = it.toString()
            requestsList.add(RpcRequestOut(GET_HISTORY_METHOD, RpcParams.listParams(addrHex)))
        }
        val transactionHistoryArray = jsonRpcTcpClient.write(requestsList, 50000).responses

        val outputs = ArrayList<TransactionHistoryInfo>()
        transactionHistoryArray.forEach {
            outputs.addAll(it.getResult(Array<TransactionHistoryInfo>::class.java)!!)
        }
        outputs.sort()
        txIds.addAll(outputs.slice(IntRange(0, Math.min(request.limit, outputs.size) - 1))
                .map { Sha256Hash.fromString(it.tx_hash) })

        return WapiResponse(QueryTransactionInventoryResponse(bestChainHeight, txIds))
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
        val txId = response.getResult(String::class.java)!!
        return WapiResponse(BroadcastTransactionResponse(true, Sha256Hash.fromString(txId)))
    }

    override fun checkTransactions(request: CheckTransactionsRequest): WapiResponse<CheckTransactionsResponse> {
        val requestsList = request.txIds.map {
            RpcRequestOut("blockchain.transaction.get",
                    RpcParams.mapParams(
                            "tx_hash" to it.toHex(),
                            "verbose" to true))
        }.toList()

        val transactionsArray = jsonRpcTcpClient.write(requestsList, 15000).responses.map {
            if (it.hasError) {
                logger.logError("checkTransactions failed: ${it.error}")
            }
            val tx = it.getResult(TransactionX::class.java)
            if (tx == null) {
                null
            } else {
                TransactionStatus(
                        Sha256Hash.fromString(tx.hash),
                        true, // TODO: check?
                        tx.time, // TODO: check?
                        bestChainHeight - tx.confirmations, // TODO: fix!!
                        2, // TODO: fix!
                        true) // TODO: fix!
            }
        }.filter { it != null }
        return WapiResponse(CheckTransactionsResponse(transactionsArray))
    }

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
        private const val FEATURES_METHOD = "server.features"
        private const val HEADRES_SUBSCRIBE_METHOD = "blockchain.headers.subscribe"
        @Deprecated("Address must be replaced with script")
        private const val GET_HISTORY_METHOD = "blockchain.address.get_history"
    }
}

data class ServerFeatures(
        @SerializedName("server_version") val serverVersion: String
)

data class TransactionX(
        @SerializedName("hash") val hash: String,
        @SerializedName("blockhash") val blockhash: String,
        @SerializedName("blocktime") val blocktime: Long,
        @SerializedName("confirmations") val confirmations: Int,
        @SerializedName("time") val time: Int
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

data class TransactionHistoryInfo(
        @SerializedName("fee") val fee: Int,
        @SerializedName("height") val height: Int,
        @SerializedName("tx_hash") val tx_hash: String
) : Comparable<TransactionHistoryInfo> {
    /**
     * Sort by height, largest height first
     */
    override fun compareTo(other: TransactionHistoryInfo): Int {
        // Make pending transaction have maximum height
        val myHeight = if (height == -1) Integer.MAX_VALUE else height
        val otherHeight = if (other.height == -1) Integer.MAX_VALUE else other.height

        return when {
            myHeight < otherHeight -> 1
            myHeight > otherHeight -> -1
            else -> 0
        }
    }

}