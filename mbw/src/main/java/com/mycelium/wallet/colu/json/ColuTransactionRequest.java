package com.mycelium.wallet.colu.json;

import java.util.List;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;

public class ColuTransactionRequest {
    public static class Json extends GenericJson {
        @Key
        public long fee;

        @Key
        public List<String> from;

        @Key
        public List<String> sendutxo;

        @Key
        public String financeOutputTxid;

        @Key
        public List<ColuTxDest.Json> to;

        @Key
        public ColuTxFlags.Json flags;
    }
}
