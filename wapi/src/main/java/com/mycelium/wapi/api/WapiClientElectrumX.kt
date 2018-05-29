package com.mycelium.wapi.api

import com.mycelium.WapiLogger
import com.mycelium.net.ServerEndpoints
import com.mycelium.wapi.api.jsonrpc.JsonRpcTcpClient
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
import info.laht.yajrpc.RpcParams


/**
 * This is a Wapi Client that avoids calls that require BQS by talking to ElectrumX for related calls
 */
class WapiClientElectrumX(serverEndpoints: ServerEndpoints, logger: WapiLogger, versionCode: String) : WapiClient(serverEndpoints, logger, versionCode) {

    val jsonRpcTcpClient = JsonRpcTcpClient("electrumx-1.mycelium.com", 50002)

    override fun queryUnspentOutputs(request: QueryUnspentOutputsRequest): WapiResponse<QueryUnspentOutputsResponse> {
//        public final Collection<Address> addresses;

//        public final int height;
//        public final Collection<TransactionOutputEx> unspent;
        return WapiResponse<QueryUnspentOutputsResponse>();
    }

    override fun queryTransactionInventory(request: QueryTransactionInventoryRequest): WapiResponse<QueryTransactionInventoryResponse> {
//        public final List<Address> addresses;
//        public final int limit;

//        public final int height;
//        public final List<Sha256Hash> txIds;

        return WapiResponse<QueryTransactionInventoryResponse>();
    }

    override fun getTransactions(request: GetTransactionsRequest): WapiResponse<GetTransactionsResponse> {
//        public final Collection<Sha256Hash> txIds;
//        public final Collection<TransactionExApi> transactions;

        return WapiResponse<GetTransactionsResponse>();
    }

    override fun broadcastTransaction(request: BroadcastTransactionRequest): WapiResponse<BroadcastTransactionResponse> {
      //  return WapiResponse(BroadcastTransactionResponse(
      //          response?.success ?: false,
      //          Sha256Hash.fromString(response?.txid)))

        return WapiResponse<BroadcastTransactionResponse>();
    }

    override fun checkTransactions(request: CheckTransactionsRequest): WapiResponse<CheckTransactionsResponse> {
//        public final List<Sha256Hash> txIds;
//        public final Collection<TransactionStatus> transactions;

        return WapiResponse<CheckTransactionsResponse>();
    }

    fun serverBanner(): String {
        val response = jsonRpcTcpClient.write("server.banner", RpcParams.listParams())
        return response.getResult(String::class.java)!!
    }
}

