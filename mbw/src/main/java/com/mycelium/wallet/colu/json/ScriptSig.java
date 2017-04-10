package com.mycelium.wallet.colu.json;

import java.math.BigDecimal;
import java.util.List;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;

/*
"scriptSig":{"asm":"3044022051c7029dffbb9b346f6e2cf5ebcd92397c984b2c773156dccebb0e457b0b8f7b022
01f8a1f41ad9b39ab5c26d7b9473b68fad10eae7c5cbfc3b7e77aee92dad9598901 029a0caa43dc2098f87d46d1b18
c1a20869c289159b1ef75124f053040a18bf6c4","hex":"473044022051c7029dffbb9b346f6e2cf5ebcd92397c984
b2c773156dccebb0e457b0b8f7b02201f8a1f41ad9b39ab5c26d7b9473b68fad10eae7c5cbfc3b7e77aee92dad95989
0121029a0caa43dc2098f87d46d1b18c1a20869c289159b1ef75124f053040a18bf6c4"},
*/

public class ScriptSig {
    public static class Json extends GenericJson {
        @Key
        public String _id;

        @Key
        public String asm;

        @Key
        public String hex;
    }
}
