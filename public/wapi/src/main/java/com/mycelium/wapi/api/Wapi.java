/*
 * Copyright 2013, 2014 Megion Research & Development GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mycelium.wapi.api;

import com.mycelium.wapi.api.request.*;
import com.mycelium.wapi.api.response.*;

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

   public final static int MAX_TRANSACTION_INVENTORY_LIMIT = 1000;
   /**
    * Get the logger configured for this {@link Wapi}
    *
    * @return the logger configured for this {@link Wapi}
    */
   WapiLogger getLogger();

   /**
    * Query the full set of unspent outputs for a set of addresses
    * Example HTTP POST:
    * curl  -k -X POST -H "Content-Type: application/json"
    *       -d '{"version":1,"addresses":["msxh4zZoVwdRXfgmAYYo2MpNrJi4snrH6C","mfv9QuzUD7ZtnHxfpVX2859hs2ZHC8TG16","mpii6kiLM5HffaJdeD4Smnpv5eWo7qfKQ5"]}'
    *       https://144.76.165.115/wapitestnet/wapi/queryUnspentOutputs
    */
   WapiResponse<QueryUnspentOutputsResponse> queryUnspentOutputs(QueryUnspentOutputsRequest request);

   /**
    * Query the transaction inventory of a set of addresses
    * Example HTTP POST:
    *curl   -k -X POST -H "Content-Type: application/json"
    *       -d '{"version":1,"addresses":["mfd7QG4vn2U4U5BgnTuw7dmjKsutDxkK6b","mysJrGMsYht9u3gBvKHFcNJsVEmaEPhUGA","mvMyQXzaHk7Z6u3vsbzT7qmQJo225ma9g3"]}'
    *       https://144.76.165.115/wapitestnet/wapi/queryTransactionInventory
    */
   WapiResponse<QueryTransactionInventoryResponse> queryTransactionInventory(QueryTransactionInventoryRequest request);

   /**
    * Get a set of transactions from a set of transaction IDs
    * Example HTTP POST:
    * curl  -k -X POST -H "Content-Type: application/json"
    *       -d '{"version":1,"txIds":["1513b9b160ef6b20bbb06b7bb6e7364e58e27e1df53f8f7e12e67f17d46ad198"]}'
    *       https://144.76.165.115/wapitestnet/wapi/getTransactions
    */
   WapiResponse<GetTransactionsResponse> getTransactions(GetTransactionsRequest request);

   /**
    * Broadcast a transaction
    * Example HTTP POST:
    * curl  -k -X POST -H "Content-Type: application/json"
    *       -d '{"version":1,"rawTransaction":"AQAAAAHqHGsQSIun5hjDDWm7iFMwm85xNLt+HBfI3LS3uQHnSQEAAABrSDBFAiEA6rlGk4wgIL3TvC2YHK4XiBW2vPYg82iCgnQi+YOUwqACIBpzVk756/07SRORT50iRZvEGUIn3Lh3bhaRE1aUMgZZASECDFl9wEYDCvB1cJY6MbsakfKQ9tbQhn0eH9C//RI2iE//////ApHwGgAAAAAAGXapFIzWtPXZR7lk8RtvE0FDMHaLtsLCiKyghgEAAAAAABl2qRSuzci59wapXUEzwDzqKV9nIaqwz4isAAAAAA=="}'
    *       https://144.76.165.115/wapitestnet/wapi/broadcastTransaction
    */
   WapiResponse<BroadcastTransactionResponse> broadcastTransaction(BroadcastTransactionRequest request);

   /**
    * Check the status of a transaction.
    * <p/>
    * This allows you to check whether it exists, has confirmed, or got its
    * timestamp updated.
    * Example HTTP POST:
    *curl   -k -X POST -H "Content-Type: application/json"
    *       -d '{"txIds":["1513b9b160ef6b20bbb06b7bb6e7364e58e27e1df53f8f7e12e67f17d46ad198"]}'
    *       https://144.76.165.115/wapitestnet/wapi/checkTransactions
    */
   WapiResponse<CheckTransactionsResponse> checkTransactions(CheckTransactionsRequest request);

   /**
    * Query exchange rates
    * <p/>
    * Query the exchange rates for available exchanges converted to a specific fiat currency
    * Example HTTP POST:
    * curl  -k -X POST -H "Content-Type: application/json"
    *       -d '{"version":1,"currency":"USD"}'
    *       https://144.76.165.115/wapitestnet/wapi/queryExchangeRates
    */
   WapiResponse<QueryExchangeRatesResponse> queryExchangeRates(QueryExchangeRatesRequest request);

   /**
    * Check if the wapi-service is running
    * <p>
    */
   WapiResponse<PingResponse> ping();
}
