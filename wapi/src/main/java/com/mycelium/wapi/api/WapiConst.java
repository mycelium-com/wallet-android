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

public interface WapiConst {
   String WAPI_BASE_PATH = "/wapi";

   interface Function {
       String QUERY_UNSPENT_OUTPUTS = "queryUnspentOutputs";
       String QUERY_TRANSACTION_INVENTORY = "queryTransactionInventory";
       String GET_TRANSACTIONS = "getTransactions";
       String BROADCAST_TRANSACTION = "broadcastTransaction";
       String CHECK_TRANSACTIONS = "checkTransactions";
       String QUERY_EXCHANGE_RATES = "queryExchangeRates";
       String PING = "ping";
       String COLLECT_ERROR = "collectError";
       String GET_VERSION_INFO = "getVersion";
       String GET_VERSION_INFO_EX = "getVersionEx";
       String GET_MINER_FEE_ESTIMATION = "getMinerFeeEstimations";
   }
}
