package com.mycelium.wallet.colu.json;

import java.util.List;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;

public class AddressTransactionsInfo {
    public static class Json extends GenericJson {
        @Key
        public String address;

        @Key
        public List<Tx.Json> transactions;

        @Key
        public List<Utxo.Json> utxos;

        @Key
        public long balance;

        @Key
        public long received;

        @Key
        public List<AssetBalance.Json> assets;

        @Key
        public int numOfTransactions;

    }
}

