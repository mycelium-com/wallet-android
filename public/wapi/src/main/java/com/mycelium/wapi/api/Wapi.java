package com.mycelium.wapi.api;

import com.mycelium.wapi.api.request.BroadcastTransactionRequest;
import com.mycelium.wapi.api.request.CheckTransactionsRequest;
import com.mycelium.wapi.api.request.GetTransactionsRequest;
import com.mycelium.wapi.api.request.QueryTransactionInventoryRequest;
import com.mycelium.wapi.api.request.QueryUnspentOutputsRequest;
import com.mycelium.wapi.api.response.BroadcastTransactionResponse;
import com.mycelium.wapi.api.response.CheckTransactionsResponse;
import com.mycelium.wapi.api.response.GetTransactionsResponse;
import com.mycelium.wapi.api.response.QueryTransactionInventoryResponse;
import com.mycelium.wapi.api.response.QueryUnspentOutputsResponse;

public interface Wapi {

   /**
    * The current version of the API
    */
   public static final int VERSION = 1;

   public final static int ERROR_CODE_SUCCESS = 0;
   public final static int ERROR_CODE_NO_SERVER_CONNECTION = 1;
   public final static int ERROR_CODE_INCOMPATIBLE_API_VERSION = 2;
   public final static int ERROR_CODE_INTERNAL_CLIENT_ERROR = 3;
   public final static int ERROR_CODE_INVALID_SESSION = 4;
   public final static int ERROR_CODE_INVALID_ARGUMENT = 5;
   public final static int ERROR_CODE_INTERNAL_SERVER_ERROR = 99;

   /**
    * Get the logger configured for this {@link Wapi}
    * 
    * @return the logger configured for this {@link Wapi}
    */
   WapiLogger getLogger();

   /**
    * Query the full set of unspent outputs for a set of addresses
    */
   WapiResponse<QueryUnspentOutputsResponse> queryUnspentOutputs(QueryUnspentOutputsRequest request);

   /**
    * Query the transaction inventory of a set of addresses
    */
   WapiResponse<QueryTransactionInventoryResponse> queryTransactionInventory(QueryTransactionInventoryRequest request);

   /**
    * Get a set of transactions from a set of transaction IDs
    */
   WapiResponse<GetTransactionsResponse> getTransactions(GetTransactionsRequest request);

   /**
    * Broadcast a transaction
    */
   WapiResponse<BroadcastTransactionResponse> broadcastTransaction(BroadcastTransactionRequest request);

   /**
    * Check the status of a transaction.
    * <p>
    * This allows you to check whether it exists, has confirmed, or got its
    * timestamp updated.
    */
   WapiResponse<CheckTransactionsResponse> checkTransactions(CheckTransactionsRequest request);

}
