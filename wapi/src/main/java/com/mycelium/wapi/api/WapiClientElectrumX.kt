package com.mycelium.wapi.api

import com.google.gson.annotations.SerializedName
import com.megiontechnologies.Bitcoins
import com.mrd.bitlib.util.HexUtils
import com.mrd.bitlib.util.Sha256Hash
import com.mycelium.WapiLogger
import com.mycelium.net.ServerEndpoints
import com.mycelium.wapi.api.jsonrpc.JsonRpcTcpClient
import com.mycelium.wapi.api.jsonrpc.RpcParams
import com.mycelium.wapi.api.jsonrpc.RpcRequestOut
import com.mycelium.wapi.api.lib.FeeEstimation
import com.mycelium.wapi.api.lib.FeeEstimationMap
import com.mycelium.wapi.api.request.*
import com.mycelium.wapi.api.response.*
import com.mycelium.wapi.model.TransactionStatus
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.concurrent.thread

/**
 * This is a Wapi Client that avoids calls that require BQS by talking to ElectrumX for related calls
 */
class WapiClientElectrumX(serverEndpoints: ServerEndpoints, logger: WapiLogger, versionCode: String) : WapiClient(serverEndpoints, logger, versionCode) {
    @Volatile private lateinit var jsonRpcTcpClient: JsonRpcTcpClient
    private var bestChainHeight = -1

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
    }

    override fun queryUnspentOutputs(request: QueryUnspentOutputsRequest): WapiResponse<QueryUnspentOutputsResponse> {
//        public final Collection<Address> addresses;

//        public final int height;
//        public final Collection<TransactionOutputEx> unspent;
        @Suppress("UseExpressionBody")
        // TODO: implement
        return super.queryUnspentOutputs(request)
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
        val response = jsonRpcTcpClient.write("blockchain.transaction.broadcast", RpcParams.listParams(txHex), 50000)
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
            if(tx == null) {
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
        val blocks : Array<Int> = arrayOf(1, 2, 3, 4, 5, 10, 15, 20) // this is what the wapi server used
        val requestsList = ArrayList<RpcRequestOut>()
        blocks.forEach { nBlocks ->
            requestsList.add(RpcRequestOut("blockchain.estimatefee", RpcParams.listParams(nBlocks)))
        }

        val estimatesArray = jsonRpcTcpClient.write(requestsList, 5000).responses

        val feeEstimationMap = FeeEstimationMap()

        estimatesArray.forEachIndexed { index, response ->
            feeEstimationMap[blocks[index]] = Bitcoins.valueOf(response.getResult(Double::class.java)!!)
        }
        return WapiResponse(MinerFeeEstimationResponse(FeeEstimation(feeEstimationMap, Date())))
    }

    fun serverFeatures(): ServerFeatures {
        val response = jsonRpcTcpClient.write("server.features", RpcParams.listParams(), 50000)
        return response.getResult(ServerFeatures::class.java)!!
    }
}

data class ServerFeatures (
    @SerializedName("server_version") val serverVersion: String
)
data class TransactionX(
        @SerializedName("hash") val hash: String,
        @SerializedName("blockhash") val blockhash: String,
        @SerializedName("blocktime") val blocktime: Long,
        @SerializedName("confirmations") val confirmations: Int,
        @SerializedName("time") val time: Int
)
