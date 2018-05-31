package com.mycelium.wallet.colu.json;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;

import java.util.List;

public class Vout {
    public static class Json extends GenericJson {
        @Key
        public long value;

        @Key
        public long n;

        @Key
        public ScriptPubKey.Json scriptPubKey;

        @Key
        public boolean used;

        @Key
        public long blockheight;

        @Key
        public List<Asset.Json> assets;
    }
}
