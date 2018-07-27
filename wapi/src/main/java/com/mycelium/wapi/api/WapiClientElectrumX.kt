package com.mycelium.wapi.api

import com.google.gson.annotations.SerializedName
import com.megiontechnologies.Bitcoins
import com.mrd.bitlib.StandardTransactionBuilder
import com.mrd.bitlib.model.OutPoint
import com.mrd.bitlib.model.Transaction
import com.mrd.bitlib.model.TransactionInput
import com.mrd.bitlib.util.ByteReader
import com.mrd.bitlib.util.HexUtils
import com.mrd.bitlib.util.Sha256Hash
import com.mycelium.WapiLogger
import com.mycelium.net.ServerEndpoints
import com.mycelium.wapi.api.jsonrpc.*
import com.mycelium.wapi.api.lib.FeeEstimation
import com.mycelium.wapi.api.lib.FeeEstimationMap
import com.mycelium.wapi.api.lib.TransactionExApi
import com.mycelium.wapi.api.request.*
import com.mycelium.wapi.api.response.*
import com.mycelium.wapi.model.TransactionOutputEx
import com.mycelium.wapi.model.TransactionStatus
import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import java.util.*
import java.util.concurrent.TimeoutException
import kotlin.collections.ArrayList

/**
 * This is a Wapi Client that avoids calls that require BQS by talking to ElectrumX for related calls
 */
class WapiClientElectrumX(
        serverEndpoints: ServerEndpoints,
        endpoints: Array<TcpEndpoint>,
        logger: WapiLogger,
        versionCode: String)
    : WapiClient(serverEndpoints, logger, versionCode) {
    @Volatile
    private var bestChainHeight = -1
    private val receiveHeaderCallback = { response: AbstractResponse ->
        val rpcResponse = response as RpcResponse
        bestChainHeight = if (rpcResponse.hasResult) {
            rpcResponse.getResult(BlockHeader::class.java)!!.height
        } else {
            rpcResponse.getParams(Array<BlockHeader>::class.java)!![0].height
        }
    }
    private val connectionManager = ConnectionManager(5, endpoints, logger)

    override fun setAppInForeground(isInForeground: Boolean) {
        connectionManager.setActive(isInForeground)
    }

    override fun setNetworkConnected(isNetworkConnected: Boolean) {
        connectionManager.setNetworkConnected(isNetworkConnected)
    }

    init {
        connectionManager.subscribe(Subscription(HEADRES_SUBSCRIBE_METHOD, RpcParams.listParams(true), receiveHeaderCallback))
    }

    override fun queryUnspentOutputs(request: QueryUnspentOutputsRequest): WapiResponse<QueryUnspentOutputsResponse> {
        try {
            val unspent: ArrayList<TransactionOutputEx> = ArrayList()
            val requestsList = ArrayList<RpcRequestOut>()
            val requestsIndexesMap = HashMap<String, Int>()
            val requestAddressesList = ArrayList(request.addresses)
            requestAddressesList.forEach {
                val addrHex = it.toString()
                requestsList.add(RpcRequestOut(LIST_UNSPENT_METHOD, RpcParams.listParams(addrHex)))
            }
            val unspentsArray = connectionManager.write(requestsList).responses

            //Fill temporary indexes map in order to find right address
            requestsList.forEachIndexed { index, req ->
                requestsIndexesMap[req.id.toString()] = index
            }
            unspentsArray.forEach { response ->
                val outputs = response.getResult(Array<UnspentOutputs>::class.java)
                outputs!!.forEach {
                    val script = StandardTransactionBuilder.createOutput(requestAddressesList[requestsIndexesMap[response.id.toString()]!!],
                            it.value, requestAddressesList[0].network).script
                    unspent.add(TransactionOutputEx(OutPoint(Sha256Hash.fromString(it.txHash), it.txPos), if (it.height > 0) it.height else -1 ,
                            it.value, script.scriptBytes,
                            script.isCoinBase))
                }
            }

            return WapiResponse(QueryUnspentOutputsResponse(bestChainHeight, unspent))
        } catch (ex: CancellationException) {
            return WapiResponse<QueryUnspentOutputsResponse>(Wapi.ERROR_CODE_NO_SERVER_CONNECTION, null)
        }
    }

    override fun queryTransactionInventory(request: QueryTransactionInventoryRequest): WapiResponse<QueryTransactionInventoryResponse> {
        try {
            val requestsList = ArrayList<RpcRequestOut>(request.addresses.size)
            request.addresses.forEach {
                val addrHex = it.toString()
                requestsList.add(RpcRequestOut(GET_HISTORY_METHOD, RpcParams.listParams(addrHex)))
            }
            val transactionHistoryArray = connectionManager.write(requestsList).responses

            val outputs = transactionHistoryArray.flatMap { it.getResult(Array<TransactionHistoryInfo>::class.java)!!.asIterable() }
            val txIds = outputs.slice(IntRange(0, Math.min(request.limit, outputs.size) - 1))
                    .map { Sha256Hash.fromString(it.tx_hash) }

            return WapiResponse(QueryTransactionInventoryResponse(bestChainHeight, txIds))
        } catch (ex: CancellationException) {
            return WapiResponse<QueryTransactionInventoryResponse>(Wapi.ERROR_CODE_NO_SERVER_CONNECTION, null)
        }
    }

    override fun getTransactions(request: GetTransactionsRequest): WapiResponse<GetTransactionsResponse> {
        return try {
            val transactions = getTransactionsWithParentLookupConverted(request.txIds.map { it.toHex() }) { tx, unconfirmedChainLength, rbfRisk ->
                val txIdString = Sha256Hash.fromString(tx.txid)
                val txHashString = Sha256Hash.fromString(tx.hash)
                TransactionExApi(
                        txIdString,
                        txHashString,
                        if (tx.confirmations > 0) bestChainHeight - tx.confirmations + 1 else -1,
                        if (tx.time == 0) (Date().time / 1000).toInt() else tx.time,
                        HexUtils.toBytes(tx.hex),
                        unconfirmedChainLength, // 0 or 1. we don't dig deeper. 1 == unconfirmed parent
                        rbfRisk)
            }
            WapiResponse(GetTransactionsResponse(transactions))
        } catch (ex: CancellationException) {
            WapiResponse<GetTransactionsResponse>(Wapi.ERROR_CODE_NO_SERVER_CONNECTION, null)
        }
    }

    override fun broadcastTransaction(request: BroadcastTransactionRequest): WapiResponse<BroadcastTransactionResponse> {
        try {
            val txHex = HexUtils.toHex(request.rawTransaction)
            val response = connectionManager.write(BROADCAST_METHOD, RpcParams.listParams(txHex))
            if (response.hasError) {
                logger.logError(response.error?.toString())
                return WapiResponse(BroadcastTransactionResponse(false, null))
            }
            val txId = response.getResult(String::class.java)!!
            return WapiResponse(BroadcastTransactionResponse(true, Sha256Hash.fromString(txId)))
        } catch (ex: CancellationException) {
            return WapiResponse<BroadcastTransactionResponse>(Wapi.ERROR_CODE_NO_SERVER_CONNECTION, null)
        }
    }

    override fun checkTransactions(request: CheckTransactionsRequest): WapiResponse<CheckTransactionsResponse> {
        try {
            // TODO: make the transaction "check" use blockchain.address.subscribe instead of repeated
            // polling of blockchain.transaction.get
            val transactionsArray = getTransactionsWithParentLookupConverted(request.txIds.map { it.toHex() }) { tx, unconfirmedChainLength, rbfRisk ->
                TransactionStatus(
                        Sha256Hash.fromString(tx.txid),
                        true,
                        if (tx.time == 0) (Date().time / 1000).toInt() else tx.time,
                        if (tx.confirmations > 0) bestChainHeight - tx.confirmations + 1 else -1,
                        unconfirmedChainLength, // 0 or 1. we don't dig deeper. 1 == unconfirmed parent
                        rbfRisk)
            }
            return WapiResponse(CheckTransactionsResponse(transactionsArray))
        } catch (ex: TimeoutException) {
            return WapiResponse<CheckTransactionsResponse>(Wapi.ERROR_CODE_NO_SERVER_CONNECTION, null)
        }
    }

    private fun <T> getTransactionsWithParentLookupConverted(
            txids: Collection<String>,
            conversion: (tx: TransactionX, unconfirmedChainLength: Int, rbfRisk: Boolean) -> T): List<T> {
        val transactionsArray = getTransactionXs(txids)
        // check for unconfirmed transactions parents for confirmations and RBF
        val neededParentTxids = getUnconfirmedTxsParents(transactionsArray)
        val relatedTransactions = getTransactionXs(neededParentTxids)
        return transactionsArray.map { tx ->
            val (unconfirmedChainLength, rbfRisk) = checkConfirmationsRbf(tx, relatedTransactions)
            conversion(tx, unconfirmedChainLength, rbfRisk)
        }
    }

    /**
     * This method is inteded to check if tx has unconfirmed parents and have Rbf risk.
     * @param tx transaction to check
     * @param relatedTransactions parent transactions
     * @return Pair<Int, Boolean>, where Int 1 if unconfirmed parrent exists and Boolean is RbfRisk indicator.
     */
    private fun checkConfirmationsRbf(tx: TransactionX, relatedTransactions: List<TransactionX>): Pair<Int, Boolean> {
        var unconfirmedChainLength = 0
        var rbfRisk = false
        if (tx.confirmations == 0) {
            // if unconfirmed chain length is one, see if it is two (or more)
            // see if it or parent is RBF
            val txParents = relatedTransactions.filter { ptx -> tx.vin.any { it.outPoint.txid.toString() == ptx.txid } }
            rbfRisk = isRbf(tx.vin) || txParents.any { ptx -> isRbf(ptx.vin) }
            if (txParents.any { ptx -> ptx.confirmations == 0 }) {
                unconfirmedChainLength = 1
            }
        }
        return Pair(unconfirmedChainLength, rbfRisk)
    }

    private fun getUnconfirmedTxsParents(transactionsArray: List<TransactionX>): List<String> {
        return transactionsArray.filter { it.confirmations == 0 }
                .flatMap { it.vin.asList() }
                .map { it.outPoint.txid.toString() }
    }

    private fun getTransactionXs(txids: Collection<String>): List<TransactionX> {
        if (txids.isEmpty()) {
            return emptyList()
        }
        val requestsList = txids.map {
            RpcRequestOut(GET_TRANSACTION_METHOD,
                    RpcParams.mapParams(
                            "tx_hash" to it,
                            "verbose" to true))
        }.toList().chunked(GET_TRANSACTION_BATCH_LIMIT)
        return requestTransactionsAsync(requestsList)
    }

    /**
     * This method is inteded to request transactions from different connections using endpoints list.
     */
    private fun requestTransactionsAsync(requestsList: List<List<RpcRequestOut>>): List<TransactionX> {
        return requestsList.pFlatMap {
            connectionManager.write(it)
                    .responses
                    .mapNotNull {
                        if (it.hasError) {
                            logger.logError("checkTransactions failed: ${it.error}")
                            null
                        } else {
                            it.getResult(TransactionX::class.java).apply {
                                // Since our electrumX does not send vin's anymore, parse transaction hex
                                // by ourselves and extract inputs information
                                val tx = Transaction.fromBytes(HexUtils.toBytes(this!!.hex))
                                this.vin = tx.inputs
                            }
                        }
                    }
        }
    }

    private fun <A, B>List<A>.pFlatMap(f: suspend (A) -> List<B>): List<B> = runBlocking {
        map { async(CommonPool) { f(it) } }
                .flatMap { it.await() }
    }

    private fun isRbf(vin: Array<TransactionInput>) = vin.any { it.isMarkedForRbf }

    override fun getMinerFeeEstimations(): WapiResponse<MinerFeeEstimationResponse> {
        try {
            val blocks: Array<Int> = arrayOf(1, 2, 3, 4, 5, 10, 15, 20) // this is what the wapi server used
            val requestsList = ArrayList<RpcRequestOut>()
            blocks.forEach { nBlocks ->
                requestsList.add(RpcRequestOut(ESTIMATE_FEE_METHOD, RpcParams.listParams(nBlocks)))
            }

            val estimatesArray = connectionManager.write(requestsList).responses

            val feeEstimationMap = FeeEstimationMap()

            estimatesArray.forEachIndexed { index, response ->
                feeEstimationMap[blocks[index]] = Bitcoins.valueOf(response.getResult(Double::class.java)!!)
            }
            return WapiResponse(MinerFeeEstimationResponse(FeeEstimation(feeEstimationMap, Date())))
        } catch (ex: CancellationException) {
            return WapiResponse<MinerFeeEstimationResponse>(Wapi.ERROR_CODE_NO_SERVER_CONNECTION, null)
        }
    }

    fun serverFeatures(): ServerFeatures {
        val response = connectionManager.write(FEATURES_METHOD, RpcParams.listParams())
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
        @Deprecated("Address must be replaced with script")
        private const val GET_HISTORY_METHOD = "blockchain.address.get_history"
        private const val GET_TRANSACTION_BATCH_LIMIT = 10
    }
}

data class ServerFeatures(
        @SerializedName("server_version") val serverVersion: String
)

data class TransactionX(
        val blockhash: String,
        val blocktime: Long,
        val confirmations: Int,
        val hash: String,
        val hex: String,
        val time: Int,
        val txid: String,
        var vin: Array<TransactionInput>
)

data class UnspentOutputs(
        @SerializedName("tx_hash") val txHash: String,
        @SerializedName("tx_pos") val txPos: Int,
        val height: Int,
        val value: Long
)

data class BlockHeader(
        val height: Int,
        val hex: String
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
