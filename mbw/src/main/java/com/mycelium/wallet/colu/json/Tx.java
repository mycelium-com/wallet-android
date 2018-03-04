package com.mycelium.wallet.colu.json;

import java.util.List;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;

/*
{"txid":"bd94dbd3697dbac1e37d04db1e96935c1e713b0afa896781e7b68d07cd049403",
"vout":0,
"scriptSig":{"asm":"3044022051c7029dffbb9b346f6e2cf5ebcd92397c984b2c773156dccebb0e457b0b8f7b022
01f8a1f41ad9b39ab5c26d7b9473b68fad10eae7c5cbfc3b7e77aee92dad9598901 029a0caa43dc2098f87d46d1b18
c1a20869c289159b1ef75124f053040a18bf6c4","hex":"473044022051c7029dffbb9b346f6e2cf5ebcd92397c984
b2c773156dccebb0e457b0b8f7b02201f8a1f41ad9b39ab5c26d7b9473b68fad10eae7c5cbfc3b7e77aee92dad95989
0121029a0caa43dc2098f87d46d1b18c1a20869c289159b1ef75124f053040a18bf6c4"},
"sequence":4294967295,
"previousOutput":{"asm":"OP_DUP OP_HASH160 1cebfabf0a1201fe29a126743511bf4a56c51c83 OP_EQUALVERIFY OP_CHECKSIG",
            "hex":"76a9141cebfabf0a1201fe29a126743511bf4a56c51c8388ac",
            "reqSigs":1,
            "type":"pubkeyhash",
            "addresses":["13dvcWk3HjbgjYjyakfDBcbPco5J5CZi5x"]},
"assets":[{"assetId":"LaA8aiRBha2BcC6PCqMuK8xzZqdA3Lb6VVv41K","amount":1000000,
    "issueTxid":"5babce48bfeecbcca827bfea5a655df66b3abd529e1f93c1264cb07dbe2bffe8","divisibility":7,
    "lockStatus":true,"aggregationPolicy":"aggregatable"}],
    "value":3000,
    "fixed":true}
*/

public class Tx {
    public static class Json extends GenericJson {
        @Key
        public String _id;

        @Key
        public String hex;

        @Key
        public String txid;

        @Key
        public String hash;

        @Key
        public int size;

        @Key
        public int vsize;

        @Key
        public int version;

        @Key
        public int locktime;

        @Key
        public boolean iosparsed;

        @Key
        public boolean ccparsed;

        @Key
        public long totalsent;

        @Key
        public int fee;

        @Key
        public boolean overflow;

        @Key
        public String blockhash;

        @Key
        public long blockheight;

        @Key
        public long time;

        @Key
        public int confirmations;

        @Key
        public List<Vin.Json> vin;

        @Key
        public List<Vout.Json> vout;

   //     @Key
   //     public ScriptSig.Json scriptSig;

    //    @Key
    //    public int sequence;


    //    @Key
    //    public PreviousOutput.Json previousOutput;


    //    @Key
    //    public List<Asset.Json> assets;

    //    @Key
    //    public int value;

    //    @Key
    //    public boolean fixed;
    }
}
