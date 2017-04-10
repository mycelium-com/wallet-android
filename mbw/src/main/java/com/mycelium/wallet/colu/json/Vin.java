package com.mycelium.wallet.colu.json;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;
import com.mycelium.wallet.colu.json.Asset;
import com.mycelium.wallet.colu.json.PreviousOutput;
import com.mycelium.wallet.colu.json.ScriptSig;

import java.util.List;


/*
        {
          "txid": "bd94dbd3697dbac1e37d04db1e96935c1e713b0afa896781e7b68d07cd049403",
          "vout": 2,
          "scriptSig": {
            "asm": "30440220270ba893b3f5264406bd35ac6ece303d17384ef02dc83dfaefda9291a1da65b70220743bbaa38a2940d7e6850d599053d3b7eeac4823a2f41774eb902ea7ae28dbda[ALL] 02e58f17376eb24b320b8a588e3428267baa067485af30903306eab6be522179aa",
            "hex": "4730440220270ba893b3f5264406bd35ac6ece303d17384ef02dc83dfaefda9291a1da65b70220743bbaa38a2940d7e6850d599053d3b7eeac4823a2f41774eb902ea7ae28dbda012102e58f17376eb24b320b8a588e3428267baa067485af30903306eab6be522179aa"
          },
          "sequence": 4294967295,
          "previousOutput": {
            "asm": "OP_DUP OP_HASH160 8334e2072e3c68ee6f2d89142aaab1dd7e849659 OP_EQUALVERIFY OP_CHECKSIG",
            "hex": "76a9148334e2072e3c68ee6f2d89142aaab1dd7e84965988ac",
            "reqSigs": 1,
            "type": "pubkeyhash",
            "addresses": [
              "1CxksVyk5Qtk3JpMioZZ4c8QdB9phsgr4N"
            ]
          },
          "assets": [
            {
              "assetId": "LaA8aiRBha2BcC6PCqMuK8xzZqdA3Lb6VVv41K",
              "amount": 560786379,
              "issueTxid": "5babce48bfeecbcca827bfea5a655df66b3abd529e1f93c1264cb07dbe2bffe8",
              "divisibility": 7,
              "lockStatus": true,
              "aggregationPolicy": "aggregatable"
            }
          ],
          "value": 3000,
          "fixed": true
        },

 */
public class Vin {
    public static class Json extends GenericJson {
        @Key
        public String _id;

        @Key
        public String hex;

        @Key
        public String txid;

        @Key
        public int vout;


        @Key
        public ScriptSig.Json scriptSig;

        @Key
        public long sequence;

        @Key
        public PreviousOutput.Json previousOutput;

        @Key
        public List<Asset.Json> assets;

        @Key
        public long value;

        @Key
        boolean fixed;

    }
}
