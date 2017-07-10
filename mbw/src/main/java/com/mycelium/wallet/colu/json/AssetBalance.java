package com.mycelium.wallet.colu.json;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;

public class AssetBalance {
    public static class Json extends GenericJson {
        @Key
        public String assetId;

        @Key
        public long balance;
    }
}