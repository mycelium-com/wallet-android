package com.mycelium.wallet.colu.json;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;

public class ColuTxDest {
    public static class Json extends GenericJson {
        @Key
        public String address;

        @Key
        public String assetId;

        @Key
        public long amount;
    }
}
