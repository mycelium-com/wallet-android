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
   }

}
