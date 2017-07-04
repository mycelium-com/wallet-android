package com.mycelium.wallet.colu.json;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;

public class ColuTxFlags {
    public static class Json extends GenericJson {
        @Key
        public boolean splitChange;
    }
}
