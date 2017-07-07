package com.mycelium.wallet.colu.json;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;

import java.util.List;

/**
 * Created by omerio on 07.07.2017.
 */

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
        public long blockHeight;

        @Key
        public List<Asset.Json> assets;
    }
}
