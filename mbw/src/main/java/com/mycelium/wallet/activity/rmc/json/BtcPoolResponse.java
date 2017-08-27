package com.mycelium.wallet.activity.rmc.json;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;

/**
 * Created by mycdev on 04.08.17.
 */

public class BtcPoolResponse {
    public static class Json extends GenericJson {
        @Key
        public double hashrate;
    }
}
