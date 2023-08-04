package com.mycelium.wapi.api

import com.google.gson.annotations.SerializedName
import com.megiontechnologies.Bitcoins
import com.mrd.bitlib.StandardTransactionBuilder
import com.mrd.bitlib.model.BitcoinTransaction
import com.mrd.bitlib.model.OutPoint
import com.mrd.bitlib.model.TransactionInput
import com.mrd.bitlib.util.HexUtils
import com.mrd.bitlib.util.Sha256Hash
import com.mycelium.net.ServerEndpoints
import com.mycelium.wapi.api.exception.RpcResponseException
import com.mycelium.wapi.api.jsonrpc.*
import com.mycelium.wapi.api.lib.FeeEstimation
import com.mycelium.wapi.api.lib.FeeEstimationMap
import com.mycelium.wapi.api.lib.TransactionExApi
import com.mycelium.wapi.api.request.*
import com.mycelium.wapi.api.response.*
import com.mycelium.wapi.model.TransactionOutputEx
import com.mycelium.wapi.model.TransactionStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger

/**
 * This is a Wapi Client that avoids calls that require BQS by talking to ElectrumX for related calls
 */
class WapiClientElectrumX @JvmOverloads constructor(
        serverEndpoints: ServerEndpoints,
        endpoints: Array<TcpEndpoint>,
        versionCode: String,
        androidApiVersion: Int,
        @Volatile private var isActive: Boolean = true
) : WapiClient(serverEndpoints, versionCode), ServerElectrumListChangedListener {
    private val logger = Logger.getLogger(WapiClientElectrumX::class.java.getSimpleName())
    @Volatile
    private var bestChainHeight = -1
    @Volatile
    private var isNetworkConnected: Boolean = true

    private val receiveHeaderCallback = { response: AbstractResponse ->
        val rpcResponse = response as RpcResponse
        bestChainHeight = if (rpcResponse.hasResult) {
            rpcResponse.getResult(BlockHeader::class.java)!!.height
        } else {
            rpcResponse.getParams(Array<BlockHeader>::class.java)!![0].height
        }
    }
    private var rpcClient = JsonRpcTcpClient(endpoints, androidApiVersion)

    private fun updateClient() {
        rpcClient.setActive(isNetworkConnected && isActive)
    }

    override fun setNetworkConnected(isNetworkConnected: Boolean) {
        this.isNetworkConnected = isNetworkConnected
        updateClient()
    }

    fun setClientIsActive(isActive: Boolean) {
        this.isActive = isActive
        updateClient()
    }

    override fun serverListChanged(newEndpoints: List<TcpEndpoint>) {
        rpcClient.endpointsChanged(newEndpoints.toTypedArray())
    }

    init {
        rpcClient.addSubscription(Subscription(HEADRES_SUBSCRIBE_METHOD, RpcParams.listParams(), receiveHeaderCallback))
        updateClient()
        rpcClient.start()
    }

    override suspend fun queryUnspentOutputs(request: QueryUnspentOutputsRequest): WapiResponse<QueryUnspentOutputsResponse> {
        if (!isNetworkConnected) {
            return WapiResponse<QueryUnspentOutputsResponse>(Wapi.ERROR_CODE_NO_SERVER_CONNECTION, null)
        }

        try {
            val unspent: ArrayList<TransactionOutputEx> = ArrayList()
            val requestsList = ArrayList<RpcRequestOut>()
            val requestsIndexesMap = HashMap<String, Int>()
            val requestAddressesList = ArrayList(request.addresses)
            requestAddressesList.forEach {
                val addrScriptHash = it.scriptHash.toHex()
                requestsList.add(RpcRequestOut(LIST_UNSPENT_METHOD, RpcParams.listParams(addrScriptHash)))
            }
            request.cancel = { cancelRequest(requestsList) }
            val unspentsArray = rpcClient.write(requestsList, MAX_RESPONSE_TIMEOUT).responses

            //Fill temporary indexes map in order to find right address
            requestsList.forEachIndexed { index, req ->
                requestsIndexesMap[req.id.toString()] = index
            }
            unspentsArray.forEach { response ->
                val outputs = response.getResult(Array<UnspentOutputs>::class.java)
                outputs?.forEach {
                    val script = StandardTransactionBuilder.createOutput(requestAddressesList[requestsIndexesMap[response.id.toString()]!!],
                            it.value, requestAddressesList[0].network).script
                    unspent.add(TransactionOutputEx(OutPoint(Sha256Hash.fromString(it.txHash), it.txPos), if (it.height > 0) it.height else -1,
                            it.value, script.scriptBytes,
                            script.isCoinBase))
                }
            }

            return WapiResponse(QueryUnspentOutputsResponse(bestChainHeight, unspent))
        } catch (ex: RpcResponseException) {
            return WapiResponse<QueryUnspentOutputsResponse>(Wapi.ERROR_CODE_NO_SERVER_CONNECTION, null)
        }
    }

    private fun cancelRequest(rpcRequestOuts:List<RpcRequestOut>){
        rpcClient.cancel(rpcRequestOuts)
    }

    override suspend fun queryTransactionInventory(request: QueryTransactionInventoryRequest): WapiResponse<QueryTransactionInventoryResponse> {
        if (!isNetworkConnected) {
            return WapiResponse<QueryTransactionInventoryResponse>(Wapi.ERROR_CODE_NO_SERVER_CONNECTION, null)
        }
        try {
            val requestsList = ArrayList<RpcRequestOut>(request.addresses.size)
            request.addresses.forEach {
                val addrScripthHash = it.scriptHash.toHex()
                requestsList.add(RpcRequestOut(GET_HISTORY_METHOD, RpcParams.listParams(addrScripthHash)))
            }
            request.cancel = { cancelRequest(requestsList) }
            val transactionHistoryArray = rpcClient.write(requestsList, MAX_RESPONSE_TIMEOUT).responses

            val outputs = transactionHistoryArray.filter { it.hasResult }.flatMap {
                it.getResult(Array<TransactionHistoryInfo>::class.java)!!.asIterable()
            }
            val isPartialResult = transactionHistoryArray.any { it.hasError && it.error!!.code == Wapi.ERROR_CODE_RESPONSE_TOO_LARGE }
            val txIds = outputs.map { Sha256Hash.fromString(it.tx_hash) }
            if (isPartialResult) {
                return WapiResponse<QueryTransactionInventoryResponse>(Wapi.ERROR_CODE_RESPONSE_TOO_LARGE, null)
            }
            return WapiResponse(QueryTransactionInventoryResponse(bestChainHeight, txIds))
        } catch (ex: RpcResponseException) {
            return WapiResponse<QueryTransactionInventoryResponse>(Wapi.ERROR_CODE_NO_SERVER_CONNECTION, null)
        }
    }

    override suspend fun getTransactions(request: GetTransactionsRequest): WapiResponse<GetTransactionsResponse> {
        if (!isNetworkConnected) {
            return WapiResponse<GetTransactionsResponse>(Wapi.ERROR_CODE_NO_SERVER_CONNECTION, null)
        }
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
        } catch (ex: RpcResponseException) {
            WapiResponse<GetTransactionsResponse>(Wapi.ERROR_CODE_NO_SERVER_CONNECTION, null)
        }
    }

    override fun broadcastTransaction(request: BroadcastTransactionRequest): WapiResponse<BroadcastTransactionResponse> {
        if (!isNetworkConnected) {
            return WapiResponse<BroadcastTransactionResponse>(Wapi.ERROR_CODE_NO_SERVER_CONNECTION, null)
        }
        try {
            val txHex = HexUtils.toHex(request.rawTransaction)
            val response = runBlocking { rpcClient.write(BROADCAST_METHOD, RpcParams.listParams(txHex), MAX_RESPONSE_TIMEOUT)  }

            // TODO return back to a single RpcResponse object instead of list
            //  as we don't use several TCP clients anymore
            return handleBroadcastResponse(listOf(response))
        } catch (ex: RpcResponseException) {
            return WapiResponse<BroadcastTransactionResponse>(Wapi.ERROR_CODE_NO_SERVER_CONNECTION, null)
        }
    }

    fun handleBroadcastResponse(responseList: List<RpcResponse>): WapiResponse<BroadcastTransactionResponse> {
        try {
            if (responseList.all { it.hasError }) {
                responseList.forEach { response -> logger.log(Level.SEVERE, response.error?.toString(), response.error) }
                val firstError = responseList[0].error
                        ?: return WapiResponse<BroadcastTransactionResponse>(Wapi.ERROR_CODE_PARSING_ERROR, null)
                val (errorCode, errorMessage) = if (firstError.code > 0) {
                    val electrumError = Wapi.ElectrumxError.getErrorByCode(firstError.code)
                    Pair(electrumError.errorCode, firstError.message)
                } else {
                    // This regexp is intended to calculate error code. Error codes are defined on bitcoind side, while
                    // message is constructed on Electrumx side, so this might change one day, so this code is not perfectly failsafe.
                    val errorMessageGroups = errorRegex.matchEntire(firstError.message)?.groups
                            ?: return WapiResponse<BroadcastTransactionResponse>(Wapi.ERROR_CODE_PARSING_ERROR, null)
                    val errorCode = errorMessageGroups[1]?.value?.toInt()
                            ?: return WapiResponse<BroadcastTransactionResponse>(Wapi.ERROR_CODE_PARSING_ERROR, null)
                    val errorMessage = errorMessageGroups[2]?.value
                            ?: return WapiResponse<BroadcastTransactionResponse>(Wapi.ERROR_CODE_PARSING_ERROR, null)
                    val error = Wapi.ElectrumxError.getErrorByCode(errorCode)
                    Pair(error.errorCode, errorMessage)
                }
                return WapiResponse<BroadcastTransactionResponse>(errorCode, errorMessage, null)
            }
            val txId = responseList.filter { !it.hasError }[0].getResult(String::class.java)!!
            return WapiResponse(BroadcastTransactionResponse(true, Sha256Hash.fromString(txId)))
        } catch (ex: Exception) {
            return WapiResponse<BroadcastTransactionResponse>(Wapi.ERROR_CODE_PARSING_ERROR, null)
        }
    }

    override suspend fun checkTransactions(request: CheckTransactionsRequest): WapiResponse<CheckTransactionsResponse> {
        if (!isNetworkConnected) {
            return WapiResponse<CheckTransactionsResponse>(Wapi.ERROR_CODE_NO_SERVER_CONNECTION, null)
        }

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
        } catch (ex: RpcResponseException) {
            return WapiResponse<CheckTransactionsResponse>(Wapi.ERROR_CODE_NO_SERVER_CONNECTION, null)
        }
    }

    @Throws(CancellationException::class)
    private suspend fun <T> getTransactionsWithParentLookupConverted(
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
            rbfRisk = isRbf(tx.vin) || txParents.any { ptx -> isRbf(ptx.vin) && ptx.confirmations == 0 }
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

    private suspend fun getTransactionXs(txids: Collection<String>): List<TransactionX> {
        if (txids.isEmpty()) {
            return emptyList()
        }
        val requestsList = txids.map {
            RpcRequestOut(GET_TRANSACTION_METHOD,
                    RpcParams.mapParams(
                            "tx_hash" to it,
                            "verbose" to true))
        }.toList().chunked(GET_TRANSACTION_BATCH_LIMIT)

        val resultList = ArrayList<TransactionX>()

        for (batch in requestsList) {
            val responses = rpcClient.write(batch, MAX_RESPONSE_TIMEOUT).responses

            val txs = responses.mapNotNull {
                if (it.hasError) {
                    logger.log(Level.INFO, "Transactions retrieval  failed: ${it.error}")
                    null
                } else {
                    it.getResult(TransactionX::class.java).apply {
                        // Since our electrumX does not send vin's anymore, parse transaction hex
                        // by ourselves and extract inputs information
                        val tx = BitcoinTransaction.fromBytes(HexUtils.toBytes(this!!.hex))
                        this.vin = tx.inputs
                    }
                }
            }
            resultList.addAll(txs)
        }
        return resultList
    }

    private fun isRbf(vin: Array<TransactionInput>) = vin.any { it.isMarkedForRbf }

    override suspend fun getMinerFeeEstimations(): WapiResponse<MinerFeeEstimationResponse> {
        try {
            val requestsList = blocks.map { nBlocks ->
                RpcRequestOut(ESTIMATE_FEE_METHOD, RpcParams.listParams(nBlocks))
            }

            val estimatesArray = rpcClient.write(requestsList, MAX_RESPONSE_TIMEOUT).responses
            val requestIdToBlocks = requestsList.map {
                it.id to (it.params as RpcListParams<*>).value[0] as Int
            }.toMap()

            val feeEstimationMap = FeeEstimationMap()

            estimatesArray.forEach { response ->
                // This might happened if server haven't got enough info.
                val result = response.getResult(Double::class.java) ?: -1.0
                if (result == -1.0) {
                    return WapiResponse<MinerFeeEstimationResponse>(Wapi.ERROR_CODE_INTERNAL_SERVER_ERROR, null)
                }
                feeEstimationMap[requestIdToBlocks[response.id]] = Bitcoins.valueOf(result)
            }
            return WapiResponse(MinerFeeEstimationResponse(FeeEstimation(feeEstimationMap, Date())))
        } catch (ex: RpcResponseException) {
            return WapiResponse<MinerFeeEstimationResponse>(Wapi.ERROR_CODE_NO_SERVER_CONNECTION, null)
        }
    }

    @Suppress("unused")
    fun serverFeatures(): ServerFeatures {
        val response = runBlocking { rpcClient.write(FEATURES_METHOD, RpcParams.listParams(), MAX_RESPONSE_TIMEOUT) }
        return response.getResult(ServerFeatures::class.java)!!
    }

    companion object {
        val blocks = listOf(1, 2, 3, 4, 5, 10, 15, 20)   // this is what the wapi server used
        private const val LIST_UNSPENT_METHOD = "blockchain.scripthash.listunspent"
        private const val ESTIMATE_FEE_METHOD = "blockchain.estimatefee"
        private const val BROADCAST_METHOD = "blockchain.transaction.broadcast"
        private const val GET_TRANSACTION_METHOD = "blockchain.transaction.get"
        private const val FEATURES_METHOD = "server.features"
        private const val HEADRES_SUBSCRIBE_METHOD = "blockchain.headers.subscribe"
        private const val GET_HISTORY_METHOD = "blockchain.scripthash.get_history"
        private const val GET_TRANSACTION_BATCH_LIMIT = 10
        private val MAX_RESPONSE_TIMEOUT = TimeUnit.MINUTES.toMillis(1)
        private val errorRegex = Regex("the transaction was rejected by network rules.\\n\\n([0-9]*): (.*)\\n.*")
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

interface ServerElectrumListChangedListener {
    fun serverListChanged(newEndpoints: List<TcpEndpoint>)
}
