package com.mycelium.wapi.api

import com.mrd.bitlib.util.Sha256Hash
import com.mycelium.WapiLogger
import com.mycelium.net.ServerEndpoints
import com.mycelium.wapi.api.request.BroadcastTransactionRequest
import com.mycelium.wapi.api.request.CheckTransactionsRequest
import com.mycelium.wapi.api.request.GetTransactionsRequest
import com.mycelium.wapi.api.request.QueryTransactionInventoryRequest
import com.mycelium.wapi.api.request.QueryUnspentOutputsRequest
import com.mycelium.wapi.api.response.BroadcastTransactionResponse
import com.mycelium.wapi.api.response.CheckTransactionsResponse
import com.mycelium.wapi.api.response.GetTransactionsResponse
import com.mycelium.wapi.api.response.QueryTransactionInventoryResponse
import com.mycelium.wapi.api.response.QueryUnspentOutputsResponse
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.*

/**
 * This is a Wapi Client that avoids calls that require BQS by talking to ElectrumX for related calls
 */
class WapiClientElectrumX(serverEndpoints: ServerEndpoints, logger: WapiLogger, versionCode: String) : WapiClient(serverEndpoints, logger, versionCode) {
    val electrumXApi = Retrofit.Builder()
            .baseUrl("https://user:pass@host:port")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ElectrumXApi::class.java)

    override fun queryUnspentOutputs(request: QueryUnspentOutputsRequest): WapiResponse<QueryUnspentOutputsResponse> {
//        public final Collection<Address> addresses;

//        public final int height;
//        public final Collection<TransactionOutputEx> unspent;
    }

    override fun queryTransactionInventory(request: QueryTransactionInventoryRequest): WapiResponse<QueryTransactionInventoryResponse> {
//        public final List<Address> addresses;
//        public final int limit;

//        public final int height;
//        public final List<Sha256Hash> txIds;
    }

    override fun getTransactions(request: GetTransactionsRequest): WapiResponse<GetTransactionsResponse> {
//        public final Collection<Sha256Hash> txIds;

//        public final Collection<TransactionExApi> transactions;
    }

    override fun broadcastTransaction(request: BroadcastTransactionRequest): WapiResponse<BroadcastTransactionResponse> {
        val response = electrumXApi.broadcastTransaction(BroadcastTxRequest(request.rawTransaction))
                .execute()
                .body()
        return WapiResponse(BroadcastTransactionResponse(
                response?.success ?: false,
                Sha256Hash.fromString(response?.txid)))
    }

    override fun checkTransactions(request: CheckTransactionsRequest): WapiResponse<CheckTransactionsResponse> {
//        public final List<Sha256Hash> txIds;

//        public final Collection<TransactionStatus> transactions;
    }
}

interface ElectrumXApi {
    @POST()
    fun broadcastTransaction(@Body broadcastTxRequest: BroadcastTxRequest): Call<BroadcastTxResponse>
}

@Suppress("unused")
abstract class JsonRpcRequest {
    val jsonrpc = "2.0"
    val id = Random().nextInt()
    abstract val method: String
    abstract val params: Map<String, Any?>
}

class BroadcastTxRequest(rawTransaction: ByteArray) : JsonRpcRequest() {
    override val method = "blockchain.transaction.broadcast"
    override val params = mapOf("raw_tx" to rawTransaction)
}
class BroadcastTxResponse(val success: Boolean, val txid: String)

