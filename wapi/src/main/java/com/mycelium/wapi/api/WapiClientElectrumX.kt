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
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.concurrent.thread

/**
 * This is a Wapi Client that avoids calls that require BQS by talking to ElectrumX for related calls
 */
class WapiClientElectrumX(serverEndpoints: ServerEndpoints, logger: WapiLogger, versionCode: String) : WapiClient(serverEndpoints, logger, versionCode) {
    @Volatile
    private lateinit var jsonRpcTcpClient: JsonRpcTcpClient
    private var blockHeight = 0

    private val receiveHeaderCallback = { responce: AbstractResponse -> blockHeight = (responce as RpcResponse).getResult(BlockHeader::class.java)!!.height }

    init {
        val latch = CountDownLatch(1)
        thread(start = true) {
            jsonRpcTcpClient = JsonRpcTcpClient("electrumx-1.mycelium.com", 50012)
            latch.countDown()
            jsonRpcTcpClient.start()
        }
        if (!latch.await(10, TimeUnit.SECONDS)) {
            throw TimeoutException("JsonRpcTcpClient failed to start within time.")
        }
        jsonRpcTcpClient.writeAsync(HEADRES_SUBSCRIBE_METHOD, RpcParams.listParams(true), receiveHeaderCallback)
    }

    override fun queryUnspentOutputs(request: QueryUnspentOutputsRequest): WapiResponse<QueryUnspentOutputsResponse> {
        val unspent: ArrayList<TransactionOutputEx> = ArrayList()
        for (address in request.addresses) {
            val addrHex = address.toString()
            val response = jsonRpcTcpClient.write(LIST_UNSPENT_METHOD, RpcParams.listParams(addrHex), 50000)
            val outputs = response.getResult(Array<UnspentOutputs>::class.java)
            val script = StandardTransactionBuilder.createOutput(address, 1000, NetworkParameters.testNetwork).script
            outputs!!.forEach {
                unspent.add(TransactionOutputEx(OutPoint(Sha256Hash.fromString(it.txHash), it.txPos), it.height,
                        it.value, script.scriptBytes,
                        script.isCoinBase))
            }
        }
//        public final int height;
//        public final Collection<TransactionOutputEx> unspent;
        @Suppress("UseExpressionBody")
        // TODO: implement
        return WapiResponse(QueryUnspentOutputsResponse(blockHeight, unspent))
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
        val txId = response.getResult(String::class.java)!!
        return WapiResponse(BroadcastTransactionResponse(true, Sha256Hash.fromString(txId)))
    }

    override fun checkTransactions(request: CheckTransactionsRequest): WapiResponse<CheckTransactionsResponse> {
//        public final List<Sha256Hash> txIds;
//        public final Collection<TransactionStatus> transactions;

        @Suppress("UseExpressionBody")
        // TODO: implement
        return super.checkTransactions(request)
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
    }
}


data class ServerFeatures(
        @SerializedName("server_version") val serverVersion: String
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
