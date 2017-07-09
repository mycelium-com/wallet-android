package com.mycelium.wallet.colu.json;

import java.math.BigDecimal;
import java.util.List;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;

public class Asset {
    public static class Json extends GenericJson {
        @Key
        public long amount;

        @Key
        public String assetId;

        @Key
        public String issueTxid;

        @Key
        public int divisibility;

        @Key
        public boolean lockStatus;
    }
}
