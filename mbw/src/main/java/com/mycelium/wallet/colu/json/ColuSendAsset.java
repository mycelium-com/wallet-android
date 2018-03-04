package com.mycelium.wallet.colu.json;


import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;

import java.util.List;

// see instead ColuTransactionRequest
public class ColuSendAsset {
    public static class Json extends GenericJson {

        @Key
        public long fee;

        @Key
        public List<String> from;
    }
}
