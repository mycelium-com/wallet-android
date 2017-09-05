package com.mycelium.wallet.colu.json;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;

public class ColuBroadcastTxId {
    public static class Json extends GenericJson {
        @Key
        public String txid;
    }
}
