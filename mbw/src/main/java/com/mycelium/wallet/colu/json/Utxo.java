package com.mycelium.wallet.colu.json;

import java.util.List;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;
import com.mycelium.wallet.colu.json.Asset;
import com.mycelium.wallet.colu.json.ScriptPubKey;

public class Utxo {
    public static class Json extends GenericJson {
        @Key
        public String _id;

        @Key
        public String txid;

        @Key
        public int index;

        @Key
        public int value;

        @Key
        public int blockheight;

        @Key
        public boolean used;

        @Key
        public List<Asset.Json> assets;

        @Key
        public ScriptPubKey.Json scriptPubKey;
    }
}
