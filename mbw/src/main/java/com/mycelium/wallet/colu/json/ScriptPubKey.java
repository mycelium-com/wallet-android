package com.mycelium.wallet.colu.json;

import java.math.BigDecimal;
import java.util.List;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;

public class ScriptPubKey {
    public static class Json extends GenericJson {
        @Key
        public String asm;

        @Key
        public String hex;

        @Key
        public String type;

        @Key
        public int reqSigs;


        @Key
        public List<String> addresses;

    }
}
