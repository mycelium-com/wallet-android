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

public class WapiConst {

   public static final String WAPI_BASE_PATH = "/wapi";

   public static class Function {

      public static final String QUERY_UNSPENT_OUTPUTS = "queryUnspentOutputs";
      public static final String QUERY_TRANSACTION_INVENTORY = "queryTransactionInventory";
      public static final String GET_TRANSACTIONS = "getTransactions";
      public static final String BROADCAST_TRANSACTION = "broadcastTransaction";
      public static final String CHECK_TRANSACTIONS = "checkTransactions";
      public static final String QUERY_EXCHANGE_RATES = "queryExchangeRates";
      public static final String PING = "ping";
      public static final String COLLECT_ERROR = "collectError";
      public static final String GET_VERSION_INFO = "getVersion";
   }

}
